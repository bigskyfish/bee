package com.floatcloud.beefz.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
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
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author floatcloud
 */
@Slf4j
public class FileDistributeUtil {

    /**
     * 上传到服务器的地址
     */
    public static final String REMOTE_FOLDER = "/mnt/bee/";

    public static final int PORT = 22;

    private ChannelSftp sftp = null;
    private ChannelExec exec = null;
    private Session session = null;

    private ServerConfigPojo serverConfigPojo;

    public FileDistributeUtil(ServerConfigPojo serverConfigPojo) {
        this.serverConfigPojo = serverConfigPojo;
    }



    /**
     * 上传多个文件到远程服务器同一目录
     * @param fileAddress 文件地址信息
     * @param sh shell脚本
     * @throws Exception IO 异常
     */
    public  void uploadFiles(List<String> fileAddress, String sh) {
        long start = System.currentTimeMillis();
        String srcPath = System.getProperty("user.dir") + File.separator +"src" + File.separator;
        connect();
        if (session == null){
            return;
        }
        try{
            sftp = (ChannelSftp) session.openChannel("sftp");
            exec = (ChannelExec) session.openChannel("exec");
            //如果文件夹不存在，则创建文件夹
            createDir(REMOTE_FOLDER);
            fileAddress.forEach(address -> {
                try {
                    File file = new File(srcPath + address);
                    FileInputStream in = new FileInputStream(file);
                    sftp.put(in, address);
                } catch (SftpException | FileNotFoundException se) {
                    se.printStackTrace();
                }
            });
            log.info(serverConfigPojo.getIp() +": 文件上传成功！！ 耗时：{"+(System.currentTimeMillis() - start)+"}ms");
            exec.setCommand(sh);
            exec.connect();
        } catch (JSchException e) {
            e.printStackTrace();
        } finally {
           disconnect();
        }
    }

    public void createDir(String createpath) {
        try {
            if (isExistDir(createpath)) {
                sftp.cd(createpath);
            } else {
                sftp.cd("/mnt/");
                sftp.mkdir("bee");
            }
        } catch (SftpException e) {
            e.printStackTrace();
        }
    }


    public boolean isExistDir(String path){
        boolean isExist=false;
        try {
            SftpATTRS sftpReturn = this.sftp.lstat(path);
            isExist = true;
            return sftpReturn.isDir();
        } catch (Exception e) {
            if ("no such file".equals(e.getMessage().toLowerCase())){
                isExist = false;
            }
            e.printStackTrace();
        }
        return isExist;

    }

    /**
     * 获取bee程序私钥
     * @return 私钥集合
     */
    public ServerCoreResponsePojo getServerCoreResponsePojo(String shell) {
        ServerCoreResponsePojo result = new ServerCoreResponsePojo();
        result.setIp(serverConfigPojo.getIp());
        connect();
        if (session == null){
            return result;
        }
        try {
            sftp = (ChannelSftp) session.openChannel("sftp");
            exec = (ChannelExec) session.openChannel("exec");
            exec.setCommand(shell);
            exec.connect(30000);
            List<String> fileLines = getFileLines("/mnt/bee/privateKey.key", "UTF-8");
            fileLines.forEach(line -> {
                if (line.contains("swarm.key")) {
                    int i = line.indexOf("{");
                    String jsonStr = line.substring(i);
                    PrivateKeyPojo parse = JSON.parseObject(jsonStr, new TypeReference<PrivateKeyPojo>() {
                    });
                    result.setIp(serverConfigPojo.getIp());
                    result.setAddress(parse.getAddress());
                    result.setPrivateKey(parse.getPrivatekey());
                }
            });
        } catch (JSchException e){
            log.error("获取密钥文件异常", e);
        }
        return result;
    }


    /**
     * 读取sftp上指定（文本）文件数据,并按行返回数据集合
     *
     * @param remoteFile
     * @param charsetName
     * @return
     */
    public List<String> getFileLines(String remoteFile, String charsetName) {
        List<String> fileData;
        try (InputStream inputStream = sftp.get(remoteFile);
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
     * 连接sftp服务器
     */
    public void connect() {
        JSch jsch = new JSch();
        try{
            //采用指定的端口连接服务器
            String username = this.serverConfigPojo.getUser();
            String host = this.serverConfigPojo.getIp();
            String password = this.serverConfigPojo.getPassword();

            this.session = jsch.getSession(username,host,this.serverConfigPojo.getPort() == 0
                    && this.serverConfigPojo.getPort() == null? PORT : this.serverConfigPojo.getPort());
            if(password != null){
                //设置登陆主机的密码
                this.session.setPassword(password);
            }
            Properties sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");
            this.session.setConfig(sshConfig);
            this.session.connect();
        }catch (JSchException e){
            log.error("SFTP服务器连接异常！！", e);
        }
    }


    /**
      * 关闭连接
      */
    public void disconnect() {
        if (this.sftp != null && this.sftp.isConnected()) {
            this.sftp.disconnect();
            if (log.isInfoEnabled()) {
                log.info("sftp is closed already");
            }
        }
        if (this.exec != null && this.exec.isConnected()) {
            this.exec.disconnect();
            if (log.isInfoEnabled()) {
                log.info("sftp is closed already");
            }
        }
        if (this.session != null &&  this.session.isConnected()){
            this.session.disconnect();
            if (log.isInfoEnabled()) {
                log.info("sshSession is closed already");
            }
        }
    }


    public static void main(String[] args) throws JSchException, SftpException {
        ServerConfigPojo serverConfigPojo = new ServerConfigPojo("47.98.53.84", "root","9ol.0p;/",22);
        FileDistributeUtil fileDistributeUtil = new FileDistributeUtil(serverConfigPojo);
        ServerCoreResponsePojo serverCoreResponsePojo = fileDistributeUtil.getServerCoreResponsePojo("chmod 777 transferPrivateKey.sh && sh transferPrivateKey.sh");
        System.out.println(serverCoreResponsePojo.toString());
    }

}
