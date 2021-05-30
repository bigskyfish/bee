package com.floatcloud.beefz.util;

import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.audio.AudioDevice;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * sftp 工具类 Created by jie on 2018/7/18.
 * @author float
 */
public class SFTPHelper implements Closeable {

    public static final Logger log = LoggerFactory.getLogger(SFTPHelper.class);
    private ChannelSftp channelSftp;
    private ChannelExec channelExec;
    private Session session;

    private ServerConfigPojo serverConfigPojo;

    public ChannelSftp getChannelSftp() {
        return channelSftp;
    }

    public ChannelExec getChannelExec() {
        return channelExec;
    }

    public Session getSession() {
        return session;
    }

    /**
     * 超时数,一分钟
     */
    private static final int TIMEOUT = 60000;

    private static final int BYTE_LENGTH = 1024;

    public SFTPHelper(ServerConfigPojo serverConfigPojo){
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
     * 获得服务器连接 注意：操作完成务必调用close方法回收资源
     *
     * @see SFTPHelper#close()
     * @return
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
     * 从sftp服务器下载指定文件到本地指定目录
     *
     * @param remoteFile 文件的绝对路径+fileName
     * @param localPath 本地临时文件路径
     * @return
     */
    public boolean get(String remoteFile, String localPath) {
        if (isConnected()) {
            try {
                channelSftp.get(remoteFile, localPath);
                return true;
            } catch (SftpException e) {
                log.error("get remoteFile:{},localPath:{}, error:{}", remoteFile, localPath, e);
            }
        }
        return false;
    }

    /**
     * 读取sftp上指定文件数据
     *
     * @param remoteFile
     * @return
     */
    public byte[] getFileByte(String remoteFile) {
        byte[] fileData;
        try (InputStream inputStream = channelSftp.get(remoteFile)) {
            byte[] ss = new byte[BYTE_LENGTH];
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int rc = 0;
            while ((rc = inputStream.read(ss, 0, BYTE_LENGTH)) > 0) {
                byteArrayOutputStream.write(ss, 0, rc);
            }
            fileData = byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            log.error("getFileData remoteFile:{},error:{}", remoteFile, e);
            fileData = null;
        }
        return fileData;
    }

    /**
     * 读取sftp上指定（文本）文件数据,并按行返回数据集合
     *
     * @param remoteFile
     * @param charsetName
     * @return
     */
    public List<String> getFileLines(String remoteFile,String charsetName) {
        List<String> fileData;
        try (InputStream inputStream = channelSftp.get(remoteFile);
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream,charsetName);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String str;
            fileData = new ArrayList<>();
            while((str = bufferedReader.readLine()) != null){
                fileData.add(str);
            }
        } catch (Exception e) {
            log.error("getFileData remoteFile:{},error:{}", remoteFile, e);
            fileData = null;
        }
        return fileData;
    }


    /**
     * 上传本地文件到sftp服务器指定目录
     *
     * @param localFile
     * @param remoteFile
     * @return
     */
    public boolean put(String localFile, String remoteFile) {
        if (isConnected()) {
            try {
                channelSftp.put(localFile, remoteFile);
                return true;
            } catch (SftpException e) {
                log.error("put localPath:{}, remoteFile:{},error:{}", localFile, remoteFile, e);
                return false;
            }
        }
        return false;
    }

    /**
     * 从sftp服务器删除指定文件
     *
     * @param remoteFile
     * @return
     */
    public boolean delFile(String remoteFile) {
        if (isConnected()) {
            try {
                channelSftp.rm(remoteFile);
                return true;
            } catch (SftpException e) {
                log.error("delFile remoteFile:{} , error:{}", remoteFile, e);
            }
        }
        return false;
    }

    /**
     * 列出指定目录下文件列表
     * @param remotePath
     * @return
     */
    public Vector ls(String remotePath){
        Vector vector = null;
        if(isConnected()){
            try {
                vector = channelSftp.ls(remotePath);
            } catch (SftpException e) {
                vector = null;
                log.error("ls remotePath:{} , error:{}",remotePath,e);
            }
        }
        return vector;
    }

    /**
     * 列出指定目录下文件列表
     * @param remotePath
     * @param filenamePattern
     * @return
     *      排除./和../等目录和链接,并且排除文件名格式不符合的文件
     */
    public List<ChannelSftp.LsEntry> lsFiles(String remotePath,Pattern filenamePattern){
        List<ChannelSftp.LsEntry> lsEntryList = null;
        if(isConnected()){
            try {
                Vector<ChannelSftp.LsEntry> vector = channelSftp.ls(remotePath);
                if(vector != null) {
                    lsEntryList = vector.stream().filter(x -> {
                        boolean match = true;
                        if(filenamePattern != null){
                            Matcher mtc = filenamePattern.matcher(x.getFilename());
                            match = mtc.find();
                        }
                        if (match && !x.getAttrs().isDir() && !x.getAttrs().isLink()) {
                            return true;
                        }
                        return false;
                    }).collect(Collectors.toList());
                }
            } catch (SftpException e) {
                lsEntryList = null;
                log.error("lsFiles remotePath:{} , error:{}",remotePath,e);
            }
        }
        return lsEntryList;
    }

    /**
     * 判断链接是否还保持
     *
     * @return
     */
    public boolean isConnected() {
        if (session.isConnected() && channelSftp.isConnected()) {
            return true;
        }
        log.info("sftp server:{} is not connected",session.getHost());
        return false;
    }

    /**
     * 关闭连接资源
     */
    @Override
    public void close() throws IOException {
        if (channelSftp != null && channelSftp.isConnected()) {
            channelSftp.quit();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        log.info("session and channel is closed");
    }

    public static void main(String[] args) {
        String srcPath = System.getProperty("user.dir") + File.separator +"src" + File.separator;
        SFTPHelper sftpHelper= null;
        try {
            sftpHelper = new SFTPHelper(new ServerConfigPojo("root", "890-iop[", "47.98.53.84", 22));
            if (sftpHelper.connection()) {
                SftpATTRS lstat = sftpHelper.channelSftp.lstat("/mnt/bee/");
                if (!lstat.isDir()){
                    sftpHelper.channelSftp.cd("/mnt/");
                    sftpHelper.channelSftp.mkdir("bee");
                }
            }
        } catch (SftpException e) {
            try {
                sftpHelper.channelSftp.cd("/mnt/");
                sftpHelper.channelSftp.mkdir("bee");
            } catch (SftpException sftpException){
                sftpException.printStackTrace();
            }
        }
        try {
            String filePath = srcPath + "setup.sh";
            sftpHelper.channelSftp.cd("/mnt/bee/");
            ChannelExec shell = (ChannelExec) sftpHelper.session.openChannel("exec");
            boolean result = sftpHelper.put(filePath, "/mnt/bee/");
            shell.setCommand("rm -rf /mnt/bee/setup.sh");
            shell.connect(100);
            System.out.println(result);
        } catch ( SftpException | JSchException e){
            e.printStackTrace();
        }
    }

}
