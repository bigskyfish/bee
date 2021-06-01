package com.floatcloud.beefz.service;

import com.floatcloud.beefz.pojo.BeeVersionPojo;
import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.floatcloud.beefz.pojo.ServerCoreResponsePojo;
import com.floatcloud.beefz.util.SFTPHelper;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
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
    public static final String REMOTE_FOLDER = "/mnt/bee/";


    private final ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(25, 100, 5,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(500), new NamedThreadFactory("bee"));

    @Value("${beedata}")
    private String fileNameStr;

    @Value("${bee.clef.version}")
    private String beeClefVersion;

    @Value("${bee.version}")
    private String beeVersion;

    @Value("${bee.clef.rpm}")
    private String beeClefRpm;

    @Value("${bee.rpm}")
    private String beeRpm;



    /**
     * 不执行删除bee
     */
    public static final String SHELL_BEE_SET_UP = "chmod 777 /mnt/bee/setup.sh && sh /mnt/bee/setup.sh ";

    /**
     * 执行删除bee
     */
    public static final String GET_PRIVATE_KEY = "chmod 777 /mnt/bee/transferPrivateKey.sh && sh /mnt/bee/transferPrivateKey.sh";

    private final ReentrantLock lock = new ReentrantLock();




    /**
     * 发送文件到远程服务器
     * @param serverList 服务集合
     */
    public void sendFileToRemote(List<ServerConfigPojo> serverList, BeeVersionPojo beeVersionPojo, Integer remove) {
        String[] split = fileNameStr.split(",");
        List<String> filenames = Stream.of(split).collect(Collectors.toList());
        final String shell = remove == 1 ? SHELL_BEE_SET_UP + "-d 1 -p " : SHELL_BEE_SET_UP + "-p ";
        if (serverList != null && !serverList.isEmpty()) {
            // 执行 派发动作
            serverList.forEach(serverConfigPojo -> {
                try {
                    if(!poolExecutor.isShutdown()) {
                        poolExecutor.execute(() -> {
                            try {
                                beeSetup(serverConfigPojo, filenames, beeVersionPojo, shell + serverConfigPojo.getPassword());
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
                    channelSftp.mkdir("bee");
                }
            }
        } catch (SftpException e) {
            try {
                channelSftp.cd("/mnt/");
                channelSftp.mkdir("bee");
            } catch (SftpException sftpException){
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
            e.printStackTrace();
        }
        return result;
    }


    /**
     * 获取秘钥
     * @param serverList 服务集合
     */
    public List<ServerCoreResponsePojo> getPrivateKey(List<ServerConfigPojo> serverList) {
        List<ServerCoreResponsePojo> result = new ArrayList<>(serverList.size()+1);
        final CountDownLatch countDownLatch = new CountDownLatch(serverList.size());
        if (serverList != null && !serverList.isEmpty()) {
            // 执行 派发动作
            serverList.forEach(serverConfigPojo -> {
                try {
                    if(!poolExecutor.isShutdown()) {
                        poolExecutor.execute(() -> {
                            SFTPHelper sftpHelper = new SFTPHelper(serverConfigPojo);
                            ServerCoreResponsePojo serverCoreResponsePojo =
                                    sftpHelper.getServerCoreResponsePojo(serverConfigPojo, GET_PRIVATE_KEY);
                            try {
                                lock.lock();
                                result.add(serverCoreResponsePojo);
                            } finally {
                                countDownLatch.countDown();
                                lock.unlock();
                            }
                        });
                    }
                } catch (Exception e) {
                    log.error("====FileDistributeUtil 上传 error====", e);
                }
            });
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            log.error("发令枪执行异常", e);
        }
        return result;
    }
}
