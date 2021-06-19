package com.floatcloud.beefz.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.floatcloud.beefz.constant.FileConstant;
import com.floatcloud.beefz.pojo.PrivateKeyPojo;
import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.floatcloud.beefz.pojo.ServerCoreResponsePojo;
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
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
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
     * 执行shell脚本
     * @param shell
     * @return
     */
    public boolean exec(String shell){
        boolean result = true;
        if(connection()){
            ChannelExec exec = getChannelExec();
            log.info("执行脚本内容为：{}", shell);
            exec.setCommand(shell);
            try {
                exec.connect();
            } catch (JSchException e){
                log.error("执行脚本失败,脚本内容：{} 错误原因：{}", shell, e );
                result = false;
            }
        }
        return result;
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
     * @param remotePath
     * @param charsetName
     * @return
     */
    public List<String> getFileLines(String remotePath, String fileName, String charsetName) {
        List<String> fileData;
        try {
            long timeNum = 0;
            while (!isExistFile(remotePath, fileName) && timeNum < 30) {
                Thread.sleep(2000);
                timeNum++;
                log.info("查找文件中....次数{}", timeNum);
            }
        } catch (SftpException | InterruptedException e){
            log.error("判断文件是否异常发生异常", e);
        }
        try (InputStream inputStream = this.channelSftp.get(remotePath + fileName);
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream,charsetName);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String str;
            fileData = new ArrayList<>();
            while((str = bufferedReader.readLine()) != null){
                fileData.add(str);
            }
        } catch (Exception e) {
            log.error("getFileData remoteFile:{},error:{}", remotePath+fileName, e);
            fileData = null;
        }
        return fileData;
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
            return vector.stream().filter(ChannelSftp.LsEntry.class::isInstance).map(ChannelSftp.LsEntry.class::cast)
                    .anyMatch((lsEntry) -> StringUtils.equals(((ChannelSftp.LsEntry) lsEntry).getFilename(), fileName));
        }
        return false;
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
     * 获取bee程序私钥
     * @return 私钥集合
     */
    public ServerCoreResponsePojo getServerCoreResponsePojo(ServerCoreResponsePojo result, String shell) {
        try {
            if (connection()) {
                String nodeName = result.getNodeName();
                String remoteKeyPath = FileConstant.REMOTE_PATH + nodeName + "/";
                if (!isExistFile(remoteKeyPath, FileConstant.PRIVATE_KEY)) {
                    ChannelExec exec = getChannelExec();
                    String shellMsg = shell != null && !shell.isEmpty() ? shell : "sh /mnt/beeCli/transferPrivateKey.sh";
                    log.info("执行脚本内容为：{}", shellMsg);
                    exec.setCommand(shellMsg);
                    exec.connect();
                }
                List<String> fileLines = getFileLines(remoteKeyPath, FileConstant.PRIVATE_KEY, "UTF-8");
                Iterator<String> iterator = fileLines.iterator();
                while (iterator.hasNext()) {
                    String line = iterator.next();
                    if (line.contains("swarm.key")) {
                        int i = line.indexOf("{");
                        String jsonStr = line.substring(i);
                        PrivateKeyPojo parse = JSON.parseObject(jsonStr, new TypeReference<PrivateKeyPojo>() {
                        });
                        result.setAddress(parse.getAddress());
                        result.setPrivateKey(parse.getPrivatekey());
                        break;
                    }
                }
            }
        } catch (JSchException | SftpException e){
            log.error("获取密钥文件异常", e);
        } finally {
            close();
        }
        return result;
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

    public static void main(String[] args) throws SftpException, InterruptedException {
        String srcPath = System.getProperty("user.dir") + File.separator +"src" + File.separator;
        SFTPHelper sftpHelper= null;
        try {
            //sftpHelper = new SFTPHelper(new ServerConfigPojo("47.98.53.84", "root","9ol.0p;/",22));
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
//        List<String> fileLines = sftpHelper.getFileLines("/mnt/bee/privateKey.key", "UTF-8");
//        fileLines.forEach(System.out::println);
        boolean isExist = false;
        while(!(isExist = sftpHelper.isExistFile("/mnt/bee/", "privateKeyss.key"))){
            Thread.sleep(20000);
            System.out.println("查找文件中....");
        }
        System.out.println("文件已存在");
    }

}
