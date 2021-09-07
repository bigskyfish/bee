package com.floatcloud.beefz.base.util;

import com.floatcloud.beefz.base.pojo.BaseServerConfigPojo;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

/**
 * @author floatcloud
 */
public class BaseSftpUtils implements Closeable {

    public static final Logger log = LoggerFactory.getLogger(BaseSftpUtils.class);
    private static final int TIMEOUT = 60000;
    private ChannelSftp channelSftp;
    private ChannelExec channelExec;
    private Session session;


    public BaseSftpUtils(BaseServerConfigPojo serverConfigPojo){
        try {
            String ip = serverConfigPojo.getIp();
            int port = serverConfigPojo.getPort() > 1 ? serverConfigPojo.getPort()  : 22;

            JSch jSch = new JSch();
            session = jSch.getSession(serverConfigPojo.getUser(), ip, port);
            if (null != serverConfigPojo.getPassword()) {
                session.setPassword(serverConfigPojo.getPassword());
            }
            session.setTimeout(TIMEOUT);
            Properties properties = new Properties();
            properties.put("StrictHostKeyChecking", "no");
            session.setConfig(properties);
        } catch (Exception e) {
            log.error("init ip:{},userName:{},password:{} error:{}",serverConfigPojo.getIp(),
                    serverConfigPojo.getUser(), serverConfigPojo.getPassword(), e);
        }
    }


    /**
     * 循环创建目录
     * @param dirs 目录层级
     * @param tempPath 目录
     * @param length 长度
     * @param index 当前位置
     */
    public void mkdirDir(String[] dirs, String tempPath, int length, int index) {
        // 以"/a/b/c/d"为例按"/"分隔后,第0位是"";顾下标从1开始
        index++;
        if (index < length) {
            // 目录不存在，则创建文件夹
            tempPath += "/" + dirs[index];
        }
        try {
            channelSftp.cd(tempPath);
            if (index < length) {
                mkdirDir(dirs, tempPath, length, index);
            }
        } catch (SftpException ex) {
            try {
                channelSftp.mkdir(tempPath);
                channelSftp.cd(tempPath);
            } catch (SftpException e) {
                e.printStackTrace();
                log.error("创建目录[{}]失败,异常信息[{}]", tempPath, e);

            }
            mkdirDir(dirs, tempPath, length, index);
        }
    }



        /**
         * 向远程指定目录上传文件
         * @param remotePath 远程服务器目录
         * @param fileNames 文件名
         */
    public void sendFileToRemote(String remotePath, String srcPath, List<String> fileNames){
        String[] dirs = remotePath.split("/");
        try {
            if (connection()) {
                SftpATTRS lstat = channelSftp.lstat(remotePath);
                if (!lstat.isDir()){
                    mkdirDir(dirs, "", dirs.length, 0);
                }
            }
        } catch (SftpException e) {
            mkdirDir(dirs, "", dirs.length, 0);
        }
        fileNames.forEach(filename ->{
            String filePath = srcPath + filename;
            try {
                channelSftp.cd(remotePath);
                FileInputStream fileInputStream = new FileInputStream(filePath);
                channelSftp.put(fileInputStream, remotePath + filename);
            } catch (SftpException | IOException sftpException) {
                sftpException.printStackTrace();
            }
        });
        close();
    }



    /**
     * 读取sftp上指定（文本）文件数据,并按行返回数据集合
     * @param remotePath 远程文件路径
     * @param fileName 文件名
     * @param charsetName 编码格式
     * @return 文件内容
     */
    public List<String> getFileLines(String remotePath, String fileName, String charsetName) {
        List<String> fileData = new ArrayList<>(16);
        try {
            if (!isExistFile(remotePath, fileName)) {
                return fileData;
            }
        } catch (SftpException e){
            log.error("判断文件是否异常发生异常", e);
        }
        try (InputStream inputStream = channelSftp.get(remotePath + fileName);
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream,charsetName);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String str;
            fileData = new ArrayList<>();
            while((str = bufferedReader.readLine()) != null){
                fileData.add(str);
            }
        } catch (Exception e) {
            log.error("getFileData remoteFile:{}/{},error:{}", remotePath, fileName, e);
            fileData = null;
        }
        return fileData;
    }



    /**
     * 获得服务器连接 注意：操作完成务必调用close方法回收资源
     * @return 是否连接成功
     */
    public boolean connection() {
        try {
            if (!isConnected()) {
                session.connect();
                channelSftp = (ChannelSftp) session.openChannel("sftp");
                channelSftp.connect();

                channelExec = (ChannelExec) session.openChannel("exec");
                channelExec.connect();

                log.info("connected to host:{},userName:{}", session.getHost(), session.getUserName());
            }
            return true;
        } catch (JSchException e) {
            log.error("connection to sftp host:{} error:{}", session.getHost(), e);
            return false;
        }
    }

    /**
     * 判断链接是否还保持
     * @return 连接成功
     */
    public boolean isConnected() {
        if (session.isConnected() && channelSftp.isConnected()) {
            return true;
        }
        log.info("sftp server:{} is not connected",session.getHost());
        return false;
    }


    /**
     * 判断文件是否存在
     * @param path 文件路径
     * @param fileName 文件名
     * @return 是否存在
     * @throws SftpException sftp异常
     */
    public boolean isExistFile(String path, String fileName) throws SftpException {
        if (connection()) {
            final Vector vector = channelSftp.ls(path);
            if (CollectionUtils.isEmpty(vector)) {
                return false;
            }
            return vector.stream().filter(ChannelSftp.LsEntry.class::isInstance)
                    .map(ChannelSftp.LsEntry.class::cast)
                    .anyMatch(lsEntry -> StringUtils.equals(((ChannelSftp.LsEntry) lsEntry).getFilename(), fileName));
        }
        return false;
    }


    @Override
    public void close() {
        if (channelSftp != null && channelSftp.isConnected()) {
            channelSftp.quit();
        }
        if (channelExec != null && channelExec.isConnected()) {
            channelExec.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        log.info("session and channel is closed");
    }


}
