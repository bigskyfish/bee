package com.floatcloud.beefz.service.v2;

import com.floatcloud.beefz.dao.Node;
import com.floatcloud.beefz.dao.NodeDao;
import com.floatcloud.beefz.dao.Server;
import com.floatcloud.beefz.dao.ServerDao;
import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.floatcloud.beefz.util.SFTPHelper;
import com.floatcloud.beefz.util.ServerTypeTransferUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author aiyuner
 */
@Service
@Slf4j
public class BeeService {

    @Autowired
    private ServerDao serverDao;

    @Autowired
    private NodeDao nodeDao;

    private final ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(16, 100, 5,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(500), new NamedThreadFactory("bee"));

    @Value("${beedata}")
    private String fileNameStr;


    /**
     * 上传到服务器的地址
     */
    public static final String REMOTE_FOLDER = "/mnt/beeCli/";

    /**
     * 导入服务器信息
     * @param servers 服务列表
     */
    public void insertServers(List<ServerConfigPojo> servers){
        if(servers != null && !servers.isEmpty()){
            servers.forEach(serverConfigPojo -> {
                Server server = ServerTypeTransferUtil.transferServer(serverConfigPojo);
                serverDao.insertSelective(server);
                if(!poolExecutor.isShutdown()) {
                    poolExecutor.execute(() -> {
                        // 上传文件
                        sendFileToRemote(server);
                    });
                }
            });
        }
    }

    public List<Server> getServers(Integer status){
        List<Server> servers;
        if (status == null){
            servers = serverDao.selectServers();
        } else {
            servers = serverDao.selectServersByStatus(status);
        }
        return servers;
    }


    /**
     * 上传文件到服务器，并且完成其基础程序的初始化
     * @param server
     */
    public void sendFileToRemote(Server server){
        ServerConfigPojo serverConfigPojo = ServerTypeTransferUtil.transferServerConfigPojo(server);
        String[] split = fileNameStr.split(",");
        List<String> filenames = Stream.of(split).collect(Collectors.toList());
        String srcPath = System.getProperty("user.dir") + File.separator +"src" + File.separator;
        SFTPHelper sftpHelper = null;
        ChannelSftp channelSftp = null;
        try {
            sftpHelper = new SFTPHelper(serverConfigPojo);
            if (sftpHelper.connection()) {
                channelSftp = sftpHelper.getChannelSftp();
                SftpATTRS lstat = channelSftp.lstat(REMOTE_FOLDER);
                if (!lstat.isDir()){
                    channelSftp.cd("/mnt/");
                    channelSftp.mkdir("beeCli");
                }
            }
        } catch (SftpException e) {
            try {
                channelSftp.cd("/mnt/");
                channelSftp.mkdir("beeCli");
            } catch (SftpException sftpException){
                sftpException.printStackTrace();
            }
        }
        ChannelSftp finalChannelSftp = channelSftp;
        filenames.forEach(filename ->{
            String filePath = srcPath + filename;
            try {
                finalChannelSftp.cd(REMOTE_FOLDER);
                FileInputStream fileInputStream = new FileInputStream(filePath);
                finalChannelSftp.put(fileInputStream, REMOTE_FOLDER + filename);
            } catch (SftpException | IOException sftpException) {
                sftpException.printStackTrace();
            }
        });
        // 执行 基础程序下载安装脚本
        execShell(serverConfigPojo, "sh /mnt/beeCli/installBase.sh");
    }


    /**
     * 发送文件到远程服务器
     * @param serverList 服务集合
     */
    public void beeSetup(List<Server> serverList, String version) {
        if (serverList != null && !serverList.isEmpty()) {
            // 执行 派发动作
            serverList.forEach(server -> {
                try {
                    ServerConfigPojo serverConfigPojo = ServerTypeTransferUtil.transferServerConfigPojo(server);
                    int nodeNum = serverConfigPojo.getNodeNum();
                    String psd = serverConfigPojo.getPassword();
                    String endPoint = serverConfigPojo.getEndPoint();
                    String ip = serverConfigPojo.getIp();
                    if(!poolExecutor.isShutdown()) {
                        poolExecutor.execute(() -> {
                            try {
                                String shell = "sh /mnt/beeCli/setup.sh " + version+ " " + psd + " " + endPoint + " " + nodeNum;
                                execShell(serverConfigPojo, shell);
                                // 插入节点表
                                Node node = new Node();
                                for (int i= 1; i <= nodeNum; i++ ){
                                    node.setNodeIp(ip);
                                    node.setNodeId(i);
                                    node.setNodeName("bee"+i);
                                    nodeDao.insertSelective(node);
                                }
                            } catch (Exception e) {
                                log.error("====SshClientUtil 执行脚本 error====", e);
                            }
                        });
                    }
                } catch (Exception e) {
                    log.error("====FileDistributeUtil 上传 error====", e);
                }
            });
        }
    }



    public void execShell(ServerConfigPojo serverConfigPojo, String sh) {
        try {
            SFTPHelper sftpHelper = new SFTPHelper(serverConfigPojo);
            ChannelExec exec = sftpHelper.getChannelExec();
            exec.setCommand(sh);
            exec.connect();
        } catch (JSchException e){
            log.error("执行shell脚本失败", e);
        }
    }

}
