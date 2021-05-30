package com.floatcloud.beefz.service;

import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.floatcloud.beefz.pojo.ServerCoreResponsePojo;
import com.floatcloud.beefz.util.FileDistributeUtil;
import com.floatcloud.beefz.util.SFTPHelper;
import com.jcraft.jsch.*;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.buf.CharsetUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
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


    /**
     * 不执行删除bee
     */
    public static final String SHELL_BEE_SET_UP = "chmod 777 /mnt/bee/setup.sh && sh /mnt/bee/setup.sh -p ";
    /**
     * 执行删除bee
     */
    public static final String SHELL_BEE_SET_UP_REMOVE = "chmod 777 /mnt/bee/setup.sh && sh /mnt/bee/setup.sh -d 1 -p ";

    /**
     * 执行删除bee
     */
    public static final String GET_PRIVATE_KEY = "chmod 777 /mnt/bee/transferPrivateKey.sh && sh /mnt/bee/transferPrivateKey.sh";

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 发送文件到远程服务器
     * @param serverList 服务集合
     */
    public void sendFileToRemote(List<ServerConfigPojo> serverList, Integer remove) {
        String[] split = fileNameStr.split(",");
        List<String> filenames = Stream.of(split).collect(Collectors.toList());
        final String shell = remove == 1 ? SHELL_BEE_SET_UP_REMOVE : SHELL_BEE_SET_UP;
        if (serverList != null && !serverList.isEmpty()) {
            // 执行 派发动作
            serverList.forEach(serverConfigPojo -> {
                try {
                    if(!poolExecutor.isShutdown()) {
                        poolExecutor.execute(() -> {
                            try {
                                beeSetup(serverConfigPojo, filenames, shell);
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

    public boolean beeSetup(ServerConfigPojo serverConfigPojo, List<String> filenames, String sh){
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
//            ChannelShell channel = (ChannelShell) sftpHelper.getSession().openChannel("shell");
//            channel.connect();
//            OutputStream outputStream = channel.getOutputStream();
            filenames.forEach(filename ->{
                String filePath = srcPath + filename;
                try {
                    FileInputStream fileInputStream = new FileInputStream(filePath);
                    finalChannelSftp.cd(REMOTE_FOLDER);
                    finalChannelSftp.put(fileInputStream, REMOTE_FOLDER + filename);
                    // 设置文件的fileformat 为 unix
//                    String cmd = "vi +':w ++ff=unix' +':q' " + REMOTE_FOLDER + filename;
//                    outputStream.write(cmd.getBytes());
//                    outputStream.flush();
                } catch (SftpException | IOException sftpException) {
                    sftpException.printStackTrace();
                } finally {
//                    try {
//                        if(outputStream != null) {
//                            outputStream.close();
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                }

            });
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
        if (serverList != null && !serverList.isEmpty()) {
            // 执行 派发动作
            serverList.forEach(serverConfigPojo -> {
                try {
                    if(!poolExecutor.isShutdown()) {
                        poolExecutor.execute(() -> {
                            FileDistributeUtil fileDistributeUtil = new FileDistributeUtil(serverConfigPojo);
                            ServerCoreResponsePojo serverCoreResponsePojo =
                                    fileDistributeUtil.getServerCoreResponsePojo( GET_PRIVATE_KEY);
                            try {
                                lock.lock();
                                result.add(serverCoreResponsePojo);
                            } finally {
                                lock.unlock();
                            }
                        });
                    }
                } catch (Exception e) {
                    log.error("====FileDistributeUtil 上传 error====", e);
                }
            });
        }
        return result;
    }
}
