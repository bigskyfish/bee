package com.floatcloud.beefz.service;

import com.floatcloud.beefz.dao.ServerDao;
import com.floatcloud.beefz.pojo.BeeVersionPojo;
import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.floatcloud.beefz.pojo.ServerCoreResponsePojo;
import com.floatcloud.beefz.sysenum.ServerStatusEnum;
import com.floatcloud.beefz.util.SFTPHelper;
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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author floatcloud
 */
@Service
@Slf4j
public class SendFileService {

    public static final String SET_UP_PATH = "/mnt/bee/setup.sh";

    /**
     * 上传到服务器的地址
     */
    public static final String REMOTE_FOLDER = "/mnt/beeCli/";


    private final ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(25, 100, 5,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(500), new NamedThreadFactory("bee"));

    @Value("${beedata}")
    private String fileNameStr;

    //@Value("${bee.clef.version}")
    private String beeClefVersion;

    //@Value("${bee.version}")
    private String beeVersion;

    // @Value("${bee.clef.rpm}")
    private String beeClefRpm;

    //@Value("${bee.rpm}")
    private String beeRpm;


    @Autowired
    private ServerDao serverDao;


    /**
     * 不执行删除bee
     */
    public static final String SHELL_BEE_SET_UP = "chmod 777 /mnt/beeCli/setup.sh && sh /mnt/beeCli/setup.sh ";

    /**
     * 执行获取私钥
     */
    public static final String GET_PRIVATE_KEY = "sh /mnt/beeCli/transferPrivateKey.sh ";

    private final ReentrantLock lock = new ReentrantLock();




    /**
     * 发送文件到远程服务器
     * @param serverList 服务集合
     */
    // @Transactional
    public void sendFileToRemote(List<ServerConfigPojo> serverList, BeeVersionPojo beeVersionPojo, Integer remove, int gas) {
        String[] split = fileNameStr.split(",");
        List<String> filenames = Stream.of(split).collect(Collectors.toList());
        if (serverList != null && !serverList.isEmpty()) {
            // 执行 派发动作
            serverList.forEach(serverConfigPojo -> {
                try {
                    if(!poolExecutor.isShutdown()) {
                        poolExecutor.execute(() -> {
                            try {
                                // String psd = serverConfigPojo.getPassword();
                                String psd = "fzh01234";
                                String shell = SHELL_BEE_SET_UP + serverConfigPojo.getEndPoint() + " " + psd
                                        + " " + beeVersionPojo.getBeginIndex() + " " + serverConfigPojo.getNodeNum() + " " + gas;
                                boolean result = beeSetup(serverConfigPojo, filenames, beeVersionPojo, shell);
                                // 启动后插入数据库
                                // Server server = ServerTypeTransferUtil.transferServer(serverConfigPojo);
                                // server.setStatus(result?1:0);
                                // serverDao.insertSelective(server);
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
        if(beeVersionPojo.getBeeClefVersion() != null && !beeVersionPojo.getBeeClefVersion().isEmpty()
                && beeVersionPojo.getBeeClefRpm() != null && !beeVersionPojo.getBeeClefRpm().isEmpty()){
            beeClefVersion = beeVersionPojo.getBeeClefVersion();
            beeClefRpm = beeVersionPojo.getBeeClefRpm();
        }
        if(beeVersionPojo.getBeeVersion() != null && !beeVersionPojo.getBeeVersion().isEmpty()
                && beeVersionPojo.getBeeRpm() != null && !beeVersionPojo.getBeeRpm().isEmpty()){
            beeVersion = beeVersionPojo.getBeeVersion();
            beeRpm = beeVersionPojo.getBeeRpm();
        }
        String shellVersion = beeClefVersion + " " + beeClefRpm + " " + beeVersion + " " + beeRpm;
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
                    if("version.txt".equals(filename)){
                        InputStream in = new ByteArrayInputStream(shellVersion.getBytes(StandardCharsets.UTF_8));
                        finalChannelSftp.put(in, REMOTE_FOLDER + filename);
                    } else {
                        FileInputStream fileInputStream = new FileInputStream(filePath);
                        finalChannelSftp.put(fileInputStream, REMOTE_FOLDER + filename);
                    }
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


    /**
     * 获取秘钥
     * @param serverList 服务集合
     */
    public List<ServerCoreResponsePojo> getPrivateKey(List<ServerConfigPojo> serverList) {
        List<ServerCoreResponsePojo> result = new ArrayList<>();
        if (serverList != null && !serverList.isEmpty()) {
            int nodeTotal = 0;
            for (ServerConfigPojo serverConfigPojo : serverList) {
                nodeTotal += serverConfigPojo.getNodeNum();
            }
            CountDownLatch countDownLatch = new CountDownLatch(nodeTotal);
            // 执行 派发动作
            serverList.forEach(serverConfigPojo -> {
                try {
                    if (!poolExecutor.isShutdown()) {
                        poolExecutor.submit(() -> {
                            Integer nodeNum = serverConfigPojo.getNodeNum();
                            for (int i = 1; i <= nodeNum; i++) {
                                SFTPHelper sftpHelper = new SFTPHelper(serverConfigPojo);
                                String beePath = "/mnt/bee" + i + "/";
                                String beeFile = "beeSetup.log";
                                ServerCoreResponsePojo serverCoreResponsePojo = new ServerCoreResponsePojo.Builder()
                                        .withIp(serverConfigPojo.getIp()).withNodeName("bee" + i)
                                        .build();
                                try {
                                    if (sftpHelper.isExistFile(beePath, beeFile)) {
                                        String shell = GET_PRIVATE_KEY + i;
                                        serverCoreResponsePojo = sftpHelper.getServerCoreResponsePojo(serverCoreResponsePojo, shell);
                                        serverCoreResponsePojo.setStatusEnum(ServerStatusEnum.INSTANCE);
                                        serverCoreResponsePojo.setStatus(ServerStatusEnum.INSTANCE.getType());
                                    } else {
                                        serverCoreResponsePojo.setIp(serverConfigPojo.getIp());
                                        serverCoreResponsePojo.setStatus(ServerStatusEnum.UN_INSTANCE.getType());
                                    }
                                } catch (SftpException e) {
                                    log.error("查询bee.yaml文件报错");
                                }
                                try {
                                    lock.lock();
                                    result.add(serverCoreResponsePojo);
                                } finally {
                                    countDownLatch.countDown();
                                    lock.unlock();
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    log.error("====FileDistributeUtil 上传 error====", e);
                }
            });
            try {
                countDownLatch.await();
            } catch (InterruptedException e){
                log.error("计数器被中断");
            }
        }
        return result;
    }

    /**
     * 重启指定服务器的bee
     * @param serverList
     * @return
     */
    public void restartBeeServer(List<ServerConfigPojo> serverList, String  all){
        shellBeeServer(serverList, all);
    }

    /**
     * shell 执行方法
     * @param serverList
     */
    public void shellBeeServer(List<ServerConfigPojo> serverList, String all){
        if(serverList != null  && !serverList.isEmpty()){
            serverList.forEach( serverConfigPojo -> poolExecutor.execute(() -> {
                if ("1".equals(all)) {
                    // 同一IP中所有节点重启
                    Integer nodeNum = serverConfigPojo.getNodeNum();
                    for(int i = 1; i <= nodeNum; i++){
                        String shell = "sh /mnt/beeCli/beeRestart.sh " + i;
                        boolean result = new SFTPHelper(serverConfigPojo).exec(shell);
                    }
                } else {
                    List<Integer> errorNode = serverConfigPojo.getErrorNode();
                    errorNode.forEach(node -> {
                        String shell = "sh /mnt/beeCli/beeRestart.sh " + node;
                        new SFTPHelper(serverConfigPojo).exec(shell);
                    });
                }
            }));
        }
    }

    /**
     * 生成转账专用文件
     * @param privateKeyList 私钥数据
     * @param ethNum 转的eth数量
     * @param gBzzNum 转的gBZZ数量
     */
    public void downLoadBeeAddress(List<ServerCoreResponsePojo> privateKeyList, String ethNum, String gBzzNum) {
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
        if(privateKeyList != null && !privateKeyList.isEmpty()){
            try {
                ethWrite = new BufferedWriter(new FileWriter(ethFile));
                gBzzWrite = new BufferedWriter(new FileWriter(gBzzFile));
                for (int i = 0; i < privateKeyList.size(); i++) {
                    StringBuilder ethStr = new StringBuilder();
                    StringBuilder gBzzStr = new StringBuilder();
                    ethStr.append("0x").append(privateKeyList.get(i).getAddress()).append(",").append(ethNum);
                    gBzzStr.append("0x").append(privateKeyList.get(i).getAddress()).append(",").append(gBzzNum);
                    if (i != privateKeyList.size() - 1) {
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
