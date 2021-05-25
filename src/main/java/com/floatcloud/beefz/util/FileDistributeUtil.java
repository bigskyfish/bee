package com.floatcloud.beefz.util;

import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;


import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author floatcloud
 */
@Slf4j
public class FileDistributeUtil {

    /**
     * 上传到服务器的地址
     */
    public static final String REMOTE_FOLDER = "/mnt/bee";

    public static final int PORT = 22;


    /**
     * 上传单个文件
     * @param sourceFile 上传的文件地址
     * @param remoteFileName 上传到SFTP服务器后的文件名
     * @throws Exception
     */
    public static void uploadFile(ServerConfigPojo serverConfigPojo, String sourceFile, String remoteFileName) throws Exception {
        long start = System.currentTimeMillis();
        InputStream fileStream = new ByteArrayInputStream(sourceFile.getBytes());
        ChannelSftp sftp = connect(serverConfigPojo);
        try{
            //如果文件夹不存在，则创建文件夹
            if(sftp.ls(REMOTE_FOLDER) == null){
                sftp.mkdir(REMOTE_FOLDER);
            }
            //切换到指定文件夹
            sftp.cd(REMOTE_FOLDER);
        }catch (SftpException e){
            //创建不存在的文件夹，并切换到文件夹
            sftp.mkdir(REMOTE_FOLDER);
            sftp.cd(REMOTE_FOLDER);
        }
        sftp.put(fileStream, remoteFileName);
        disconnect(sftp);
        log.info("文件上传成功！！ 耗时：{"+(System.currentTimeMillis() - start)+"}ms");
    }


    /**
     * 上传单个文件
     * @param fileStream 上传的文件地址
     * @param remoteFileName 上传到SFTP服务器后的文件名
     * @throws Exception
     */
    public static void uploadFile(ServerConfigPojo serverConfigPojo,FileInputStream fileStream, String remoteFileName) throws Exception {
        long start = System.currentTimeMillis();
        ChannelSftp sftp = connect(serverConfigPojo);
        try{
            //如果文件夹不存在，则创建文件夹
            if(sftp.ls(REMOTE_FOLDER) == null){
                sftp.mkdir(REMOTE_FOLDER);
            }
            //切换到指定文件夹
            sftp.cd(REMOTE_FOLDER);
        }catch (SftpException e){
            //创建不存在的文件夹，并切换到文件夹
            sftp.mkdir(REMOTE_FOLDER);
            sftp.cd(REMOTE_FOLDER);
        }
        sftp.put(fileStream, remoteFileName);
        disconnect(sftp);
        log.info("文件上传成功！！ 耗时：{"+(System.currentTimeMillis() - start)+"}ms");
    }

    /**
     * 删除文件
     * @param deleteFile 要删除的文件
     *
     * @throws Exception
     */
    public void delete(String deleteFile, ServerConfigPojo serverConfigPojo) throws Exception {
        ChannelSftp sftp = connect(serverConfigPojo);
        sftp.cd(REMOTE_FOLDER);
        sftp.rm(deleteFile);
        disconnect(sftp);
    }

    /**
     * 连接sftp服务器
     *
     * @throws Exception
     */
    public static ChannelSftp connect(ServerConfigPojo serverConfigPojo) throws Exception {
        JSch jsch = new JSch();
        try{
            //采用指定的端口连接服务器
            String username = serverConfigPojo.getUser();
            String host = serverConfigPojo.getIp();
            String password = serverConfigPojo.getPassword();
            Session session = jsch.getSession(username,host,PORT);
            if(password != null){
                //设置登陆主机的密码
                session.setPassword(password);
            }

            Properties sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");
            session.setConfig(sshConfig);
            session.connect();

            //创建sftp通信通道
            Channel channel = session.openChannel("sftp");
            channel.connect();
            log.info("sftp server connect success !!");
            return (ChannelSftp) channel;
        }catch (JSchException e){
            log.error("SFTP服务器连接异常！！", e);
            throw new Exception("SFTP服务器连接异常！！",e);
        }
    }


    /**
     * Disconnect with server
     *
     * @throws Exception
     */
    public static void disconnect(ChannelSftp sftp) throws Exception {
        if (sftp != null) {
            if (sftp.isConnected()) {
                sftp.disconnect();
            } else if (sftp.isClosed()) {
            }
        }
    }
}
