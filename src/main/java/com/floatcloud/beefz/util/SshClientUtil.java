package com.floatcloud.beefz.util;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import com.floatcloud.beefz.pojo.ServerConfigPojo;
import lombok.Data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * @author floatcloud
 */
@Data
public class SshClientUtil {


    private String charset = Charset.defaultCharset().toString();
    private static final int TIME_OUT = 1000 * 10 * 60;

    private Connection conn;


    /**
     * 登录指远程服务器
     * @return
     * @throws IOException
     */
    private boolean login(ServerConfigPojo serverConfigPojo) throws IOException {
        conn = new Connection(serverConfigPojo.getIp());
        conn.connect();
        return conn.authenticateWithPassword(serverConfigPojo.getUser(), serverConfigPojo.getPassword());
    }

    public int exec(ServerConfigPojo serverConfigPojo, String shell) {
        int ret = -1;
        try {
            if (login(serverConfigPojo)) {
                Session session = conn.openSession();
                session.execCommand(shell , "UTF-8" );
                session.waitForCondition(ChannelCondition.EXIT_STATUS, TIME_OUT);
                ret = session.getExitStatus();
            } else {
                // 自定义异常类 实现略
                throw new RuntimeException("登录远程机器失败" + serverConfigPojo.getIp());
            }
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        return ret;
    }

    public String execForBack(ServerConfigPojo serverConfigPojo, String shell) {
        StringBuffer stringBuffer = new StringBuffer();
        try {
            if (login(serverConfigPojo)) {
                Session session = conn.openSession();
                session.execCommand(shell , "UTF-8" );
                session.waitForCondition(ChannelCondition.EXIT_STATUS, TIME_OUT);
                InputStream stdout = session.getStdout();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stdout));
                String line;
                while((line = bufferedReader.readLine()) != null){
                    stringBuffer.append(line).append("\n");
                }
            } else {
                // 自定义异常类 实现略
                throw new RuntimeException("登录远程机器失败" + serverConfigPojo.getIp());
            }
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        return stringBuffer.toString();
    }


    public static void main(String[] args) {
        String shell = "docker --version";
        SshClientUtil sshClientUtil = new SshClientUtil();
        ServerConfigPojo  serverConfigPojo1 = new ServerConfigPojo();
        ServerConfigPojo  serverConfigPojo2 = new ServerConfigPojo();
        serverConfigPojo1.setUser("root");
        serverConfigPojo1.setPassword("Szgy1314@");
        serverConfigPojo1.setPort(22);
        serverConfigPojo1.setIp("36.137.173.34");
        serverConfigPojo2.setUser("root");
        serverConfigPojo2.setPassword("Szgy1314@");
        serverConfigPojo2.setPort(22);
        serverConfigPojo2.setIp("36.137.171.40");
        System.out.println("==="+sshClientUtil.execForBack(serverConfigPojo1, shell));
        System.out.println("==="+sshClientUtil.execForBack(serverConfigPojo2, shell));
    }
}
