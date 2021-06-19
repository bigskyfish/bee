package com.floatcloud.beefz.service.v2;

import com.floatcloud.beefz.dao.Server;
import com.floatcloud.beefz.dao.ServerDao;
import com.floatcloud.beefz.pojo.BeeVersionPojo;
import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.floatcloud.beefz.util.SFTPHelper;
import com.floatcloud.beefz.util.ServerTypeTransferUtil;
import com.jcraft.jsch.*;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
     * @param servers
     */
    public void insertServers(List<ServerConfigPojo> servers){
        if(servers != null && !servers.isEmpty()){
            servers.forEach(serverConfigPojo -> {
                Server server = ServerTypeTransferUtil.transferServer(serverConfigPojo);
                serverDao.insertSelective(server);
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
     * 发送文件到远程服务器
     * @param serverList 服务集合
     */
    public void beeSetup(List<ServerConfigPojo> serverList, BeeVersionPojo beeVersionPojo, Integer remove, int gas) {
        String[] split = fileNameStr.split(",");
        List<String> filenames = Stream.of(split).collect(Collectors.toList());
        if (serverList != null && !serverList.isEmpty()) {
            // 执行 派发动作
            serverList.forEach(serverConfigPojo -> {
                try {
                    if(!poolExecutor.isShutdown()) {
                        poolExecutor.execute(() -> {
                            try {
                                String shell = "sh setup.sh ethersphere/bee:latest fzh123456 https://goerli.infura.io/v3/02e662f78f85462489e24897d17e0f72 6";
                                boolean result = beeSetup(serverConfigPojo, filenames, beeVersionPojo, shell);
                                // TODO 插入节点表

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


    public boolean beeSetup(ServerConfigPojo serverConfigPojo, List<String> filenames, BeeVersionPojo beeVersionPojo, String sh){
        boolean result = true;
        String srcPath = System.getProperty("user.dir") + File.separator +"src" + File.separator;
        SFTPHelper sftpHelper= null;
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
                result = false;
                sftpException.printStackTrace();
            }
        }
        try {
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
            log.info("=======执行脚本开始=========");
            log.info("=======执行脚本内容："+sh+" =========");
            ChannelExec exec = sftpHelper.getChannelExec();
            exec.setCommand(sh);
            exec.connect();
        } catch (JSchException e){
            result= false;
            e.printStackTrace();
        }
        return result;
    }


}
