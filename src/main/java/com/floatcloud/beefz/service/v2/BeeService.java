package com.floatcloud.beefz.service.v2;

import com.floatcloud.beefz.dao.Node;
import com.floatcloud.beefz.dao.NodeDao;
import com.floatcloud.beefz.dao.Server;
import com.floatcloud.beefz.dao.ServerDao;
import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.floatcloud.beefz.pojo.ServerCoreResponsePojo;
import com.floatcloud.beefz.util.SFTPHelper;
import com.floatcloud.beefz.util.ServerTypeTransferUtil;
import com.floatcloud.beefz.util.SshClientUtil;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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

    @Value("${host}")
    private String localhost;

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
                List<Server> serverList = serverDao.selectServersByIp(server.getIp());
                if(serverList == null || serverList.isEmpty()) {
                    serverDao.insertSelective(server);
                }
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
     * 启动传递地址脚本
     */
    public void setupGetAddress(){
        List<Server> servers = getServers(1);
        if(servers != null && !servers.isEmpty()) {
            servers.forEach(server -> {
                ServerConfigPojo serverConfigPojo = ServerTypeTransferUtil.transferServerConfigPojo(server);
                if (!poolExecutor.isShutdown()) {
                    poolExecutor.execute(() -> doExecShell2(serverConfigPojo, "chmod 777 /mnt/beeCli/address.sh && sh /mnt/beeCli/address.sh "
                    + localhost + " " + serverConfigPojo.getIp() + " " + serverConfigPojo.getNodeNum()));
                }
            });

        }
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
        if(!poolExecutor.isShutdown()) {
            poolExecutor.execute(() -> {
                doExecShell2(serverConfigPojo, "chmod 777 /mnt/beeCli/install.sh && nohup sh /mnt/beeCli/install.sh /dev/null &");
            });
        }
        // 更新服务器信息状态
        serverDao.updateStatus(serverConfigPojo.getIp(), 1);
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
                                String shell = "nohup sh /mnt/beeCli/setup.sh " + version+ " " + psd + " " + endPoint + " " + nodeNum
                                        + " >>/mnt/beeCli/setup.log 2>&1 &" ;
                                doExecShell2(serverConfigPojo, shell);
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





    public void doExecShell(ServerConfigPojo serverConfigPojo, String sh) {
        SFTPHelper sftpHelper = null;
        try {
            sftpHelper = new SFTPHelper(serverConfigPojo);
            if (sftpHelper.connection()) {
                log.info("====执行shell====："+sh);
                ChannelExec exec = sftpHelper.getChannelExec();
                exec.setCommand(sh);
                exec.connect();
            }
        } catch (JSchException e){
            log.error("执行shell脚本失败", e);
        } finally {
            if(sftpHelper != null){
                sftpHelper.close();
            }
        }
    }

    public void doExecShell2(ServerConfigPojo serverConfigPojo, String sh) {
        SshClientUtil sshClientUtil = new SshClientUtil();
        sshClientUtil.exec(serverConfigPojo, sh);
    }


    public void updateBeeNode(String ip, String addressStr){
        if(addressStr != null && !addressStr.isEmpty()){
            String[] stirs = addressStr.split(";");
            if(stirs.length != 0){
               for(String str : stirs){
                   if(str != null && !str.isEmpty()){
                       String[] split = str.split(",");
                       int id = Integer.parseInt(split[0]);
                       if(id != 0) {
                           String address = split[1].endsWith("\"")?split[1].substring(1, split[1].length()-1):split[1];
                           nodeDao.updateAddressByIpAndId(ip, id, address);
                       } else {
                           log.error("==============获取地址出错===========");
                       }
                   }
               }
            }
        }
    }


    public List<ServerCoreResponsePojo> getAddress(){
        List<ServerCoreResponsePojo> response = new ArrayList<>();
        List<Node> nodes = nodeDao.selectAll();
        if(nodes != null && !nodes.isEmpty()){
            nodes.forEach(node -> {
                ServerCoreResponsePojo serverCoreResponsePojo = new ServerCoreResponsePojo.Builder()
                        .withIp(node.getNodeIp()).withNodeName(node.getNodeName())
                        .withAddress(node.getNodeAddress()).build();
                response.add(serverCoreResponsePojo);
            });
        }
        return response;
    }


    /**
     * 生成转账专用文件
     * @param addresses 私钥数据
     * @param ethNum 转的eth数量
     * @param gBzzNum 转的gBZZ数量
     */
    public void downLoadBeeAddress(List<ServerCoreResponsePojo> addresses, String ethNum, String gBzzNum) {
        String filePath = System.getProperty("user.dir") + File.separator;
        File ethFile = new File (filePath+"eth.csv");
        File gBzzFile = new File(filePath+"gBzz.csv");
        if(!ethFile.exists()){
            try {
                ethFile.createNewFile();
            } catch (IOException e) {
                log.error("创建eth文件异常");
            }
        }
        if(!gBzzFile.exists()){
            try {
                gBzzFile.createNewFile();
            } catch (IOException e) {
                log.error("创建gBzz文件异常");
            }
        }
        BufferedWriter ethWrite = null;
        BufferedWriter gBzzWrite = null;
        if(addresses != null && !addresses.isEmpty()){
            try {
                ethWrite = new BufferedWriter(new FileWriter(ethFile));
                gBzzWrite = new BufferedWriter(new FileWriter(gBzzFile));
                for (int i = 0; i < addresses.size(); i++) {
                    StringBuilder ethStr = new StringBuilder();
                    StringBuilder gBzzStr = new StringBuilder();
                    ethStr.append(addresses.get(i).getAddress()).append(",").append(ethNum);
                    gBzzStr.append(addresses.get(i).getAddress()).append(",").append(gBzzNum);
                    if (i != addresses.size() - 1) {
                        ethStr.append("\r");
                        gBzzStr.append("\r");
                    }
                    ethWrite.write(ethStr.toString());
                    gBzzWrite.write(gBzzStr.toString());
                    ethWrite.flush();
                    gBzzWrite.flush();
                }
            } catch (IOException e){
                log.error("写入eth、gBzzAddress文件失败");
            } finally {
                if(ethWrite != null){
                    try {
                        ethWrite.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                if(gBzzWrite != null){
                    try {
                        gBzzWrite.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }

    }
}
