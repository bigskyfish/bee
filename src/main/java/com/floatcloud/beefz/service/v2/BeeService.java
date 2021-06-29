package com.floatcloud.beefz.service.v2;

import com.floatcloud.beefz.dao.Node;
import com.floatcloud.beefz.dao.NodeDao;
import com.floatcloud.beefz.dao.Server;
import com.floatcloud.beefz.dao.ServerDao;
import com.floatcloud.beefz.pojo.ServerConfigPojo;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
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

    private final ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(16, 100, 30,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(500), new NamedThreadFactory("bee"));

    @Value("${beedata}")
    private String fileNameStr;


    /**
     * 上传到服务器的地址
     */
    public static final String REMOTE_FOLDER = "/mnt/beeCli/";

    public static final String SHELL = "chmod 777 /mnt/beeCli/connect.sh && sh /mnt/beeCli/connect.sh ";

    /**
     * 导入服务器信息
     *
     * @param servers 服务列表
     */
    public void insertServers(List<ServerConfigPojo> servers) {
        if (servers != null && !servers.isEmpty()) {
            servers.forEach(serverConfigPojo -> {
                Server server = ServerTypeTransferUtil.transferServer(serverConfigPojo);
                List<Server> serverList = serverDao.selectServersByIp(server.getIp());
                if (serverList == null || serverList.isEmpty()) {
                    serverDao.insertSelective(server);
                }
                sendFileToRemote(server);
            });
        }
    }

    public void sendFiles(List<String> ips, String fileList) {
        if (ips != null && !ips.isEmpty()) {
            ips.forEach(ip -> {
                log.info("=========执行查询的ip为：=======" + ip);
                List<Server> servers = serverDao.selectServersByIp(ip);
                log.info("=========执行查询的结果：=======" + servers.size());
                if (servers != null && servers.size() == 1) {
                    ServerConfigPojo serverConfigPojo = ServerTypeTransferUtil.transferServerConfigPojo(servers.get(0));
                    log.info(String.format("======执行文件发送：ip为%s ；文件为%s ======", serverConfigPojo.getIp(), fileList));
                    if (!poolExecutor.isShutdown()) {
                        poolExecutor.execute(() -> sendFileToRemote(serverConfigPojo, fileList));
                    }
                }
            });
        }
    }


    public List<Server> getServers(Integer status) {
        List<Server> servers;
         if (status == null) {
            servers = serverDao.selectServers();
        } else {
            servers = serverDao.selectServersByStatus(status);
        }
        return servers;
    }

    public List<String> getErrorServers() throws InterruptedException {
        List<String> result = new ArrayList<>();
        List<Server> servers = getServers(null);
        CountDownLatch countDownLatch = new CountDownLatch(servers.size());
        if (servers != null && !servers.isEmpty()) {
            servers.forEach(server -> {
                ServerConfigPojo serverConfigPojo = ServerTypeTransferUtil.transferServerConfigPojo(server);
                if (!poolExecutor.isShutdown()) {
                    poolExecutor.execute(() -> {
                        String back = doExecShellForBack(serverConfigPojo, "docker --version");
                        log.info(String.format("******ip：%s ******* 返回值：%s ******", serverConfigPojo.getIp(), back));
                        if(back == null || back.isEmpty()){
                            result.add(serverConfigPojo.getIp());
                        }
                        countDownLatch.countDown();
                    });
                }
            });
        }
        countDownLatch.await();
        return result;
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
                    poolExecutor.execute(() -> doExecShell2(serverConfigPojo, "nohup sh /mnt/beeCli/address.sh "
                            + localhost + " " + serverConfigPojo.getIp() + " " + serverConfigPojo.getNodeNum()
                            + " >/mnt/beeCli/address.log 2>&1 &"));
                }
            });

        }
    }

    public void shellAllNode(String shell){
        List<Server> servers = getServers(null);
        if(servers != null && !servers.isEmpty()) {
            servers.forEach(server -> {
                ServerConfigPojo serverConfigPojo = ServerTypeTransferUtil.transferServerConfigPojo(server);
                if (!poolExecutor.isShutdown()) {
                    poolExecutor.execute(() -> doExecShell2(serverConfigPojo, shell));
                }
            });

        }
    }


    public void listenBeeStatus(){
        List<Server> servers = getServers(1);
        if(servers != null && !servers.isEmpty()) {
            servers.forEach(server -> {
                ServerConfigPojo serverConfigPojo = ServerTypeTransferUtil.transferServerConfigPojo(server);
                log.info("=====**监控执行**的服务器信息：=====" + serverConfigPojo.toString());
                if (!poolExecutor.isShutdown()) {
                    poolExecutor.execute(() -> doExecShell2(serverConfigPojo, "nohup sh /mnt/beeCli/listenBee.sh "
                            + localhost + " " + serverConfigPojo.getIp() + " " + serverConfigPojo.getNodeNum()
                            + " >/mnt/beeCli/listenBee.log 2>&1 &"));
                }
            });
        }
    }


    public void connectBootNode(List<String> ipList){
        List<Server> servers;
        if (ipList != null && !ipList.isEmpty()){
            log.info("====手动引导连接开始=====");
            servers = serverDao.selectServersByIpList(ipList);
        } else {
            log.info("====自动引导连接开始=====");
            servers = serverDao.selectNeedConnectServers();
        }
        if(servers != null && !servers.isEmpty()){
            log.info("=====进入引导逻辑=====");
            servers.forEach(server -> {
                ServerConfigPojo configPojo = ServerTypeTransferUtil.transferServerConfigPojo(server);
                log.info("====引导的服务器信息：===" + configPojo.toString());
                doExecShell2(configPojo, SHELL + configPojo.getNodeNum());
                int status = nodeDao.updateStatusByIp(configPojo.getIp());
                log.info("====脚本执行成功，且修改状态是否成功：===" + status);
            });
        } else {
            log.info("========暂无需要连接引导的服务器信息======");
        }
    }


    public void sendFileToRemote(ServerConfigPojo serverConfigPojo, String fileList){
        String[] split = fileList.split(",");
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
        sftpHelper.close();
    }

    /**
     * 上传文件到服务器，并且完成其基础程序的初始化
     * @param server 服务器信息
     */
    public void sendFileToRemote(Server server){
        ServerConfigPojo serverConfigPojo = ServerTypeTransferUtil.transferServerConfigPojo(server);

        if(!poolExecutor.isShutdown()) {
            poolExecutor.execute(() -> {
                    sendFileToRemote(serverConfigPojo, fileNameStr);
                    // 执行 基础程序下载安装脚本
                    log.info("====执行基础程序安装====" + server.getIp());
                    doExecShell2(serverConfigPojo, "chmod 777 /mnt/beeCli/install.sh && nohup sh /mnt/beeCli/install.sh /dev/null &");
            });
        }
        // 更新服务器信息状态
        // serverDao.updateStatus(serverConfigPojo.getIp(), 1);
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
                    log.info("===服务器信息为===" + serverConfigPojo.toString());
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
                    serverDao.updateStatus(serverConfigPojo.getIp(), 1);
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
        log.info("===执行脚本内容为：" + sh);
        sshClientUtil.exec(serverConfigPojo, sh);
    }

    public String doExecShellForBack(ServerConfigPojo serverConfigPojo, String sh) {
        SshClientUtil sshClientUtil = new SshClientUtil();
        log.info("===执行脚本内容为：" + sh);
        return sshClientUtil.execForBack(serverConfigPojo, sh);
    }



    public void updateBeeNode(String ip, String addressStr){
        log.info("=======地址值为：=====" + addressStr);
        if(addressStr != null && !addressStr.isEmpty()){
            String[] stirs = addressStr.split(";");
            if(stirs.length != 0){
               for(String str : stirs){
                   if(str != null && !str.isEmpty()){
                       String[] split = str.split(",");
                       int id = Integer.parseInt(split[0]);
                       if(id != 0 && split.length > 1 && !split[1].isEmpty()) {
                           String address = split[1].endsWith("\"")?split[1].substring(1, split[1].length()-1):split[1];
                           address = "0x" + address;
                           nodeDao.updateAddressByIpAndId(ip, id, address);
                       } else {
                           log.info("======错误地址信息：=====" + str);
                           log.error("==============获取地址出错===========");
                       }
                   }
               }
            }
        }
    }


    public List<Node> getAddress(){
        List<Node> nodes = nodeDao.selectAll();
        if(nodes != null && !nodes.isEmpty()){
            nodes.forEach(node -> {
                node.setNodePrivateKey("******");
                node.setNodeDetail("******");
            });
        }
        return nodes;
    }


    /**
     * 生成转账专用文件
     * @param addresses 私钥数据
     * @param ethNum 转的eth数量
     * @param gBzzNum 转的gBZZ数量
     */
    public void downLoadBeeAddress(List<Node> addresses, String ethNum, String gBzzNum) {
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
                    ethStr.append(addresses.get(i).getNodeAddress()).append(",").append(ethNum);
                    gBzzStr.append(addresses.get(i).getNodeAddress()).append(",").append(gBzzNum);
                    if (i != addresses.size() - 1) {
                        ethStr.append("\n");
                        gBzzStr.append("\n");
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


    public void updateBeeStatus(String ip, String stop, String running) {
        log.info(String.format("==ip: %s==;==stop: %s==;==running: %s==", ip, stop, running));
        if(ip != null && !ip.isEmpty()){
            if (stop != null && !stop.isEmpty()){
                ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(stop.split(",")));
                ArrayList<Integer> integerArrayList = new ArrayList<>();
                arrayList.forEach(ipStr -> integerArrayList.add(Integer.parseInt(ipStr)));
                log.info("====停止的bee服务，修改状态===");
                int num = 0;
                if(arrayList.size() > 1) {
                    num = nodeDao.updateStatusByIpAndIds(ip, integerArrayList);
                } else {
                    num = nodeDao.updateStatusByIpAndId(ip, integerArrayList.get(0));
                }
                log.info("====停止的bee服务，修改状态成功，条数：===" + num);
            }
            Node node = new Node();
            if (running != null && !running.isEmpty()){
                String[] split = running.split(";");
                if( split.length > 0){
                    for (String info: split) {
                        String[] msg = info.split(",");
                        if(msg.length == 2){
                            node.setNodeId(Integer.parseInt(msg[0]));
                            node.setNodeIp(ip);
                            node.setConnectPeers(Integer.parseInt(msg[1]));
                            node.setNodeStatus(1);
                            node.setRunMinute(5);
                            int num = nodeDao.updateByIpAndIdSelective(node);
                            log.info("====启动的bee服务，修改状态成功，条数：===" + num);
                        }
                    }
                }
            }
        }
    }



    public void updateServerStatus(String ip, String cpu,String memory, String memoryUsed, String disk, String diskUsed, String bandWidth) {
        log.info(String.format("==ip: %s==;==cpu: %s==;==memory: %s==;==disk: %s==;==bandWidth: %s==", ip, cpu, memoryUsed, diskUsed, bandWidth));
        if(ip != null && !ip.isEmpty()){
            // TODO 修改监控数据
            Server server = new Server();
            server.setIp(ip);
            server.setCpuUsed(cpu);
            server.setMemory(memory);
            server.setMemoryUsed(memoryUsed);
            server.setAllDisk(disk);
            server.setDiskUsed(diskUsed);
            server.setBandWidth(bandWidth);
            log.info("===修改数据库的Server===：" + server.toString());
            serverDao.updateByIpSelective(server);
        }
    }
}
