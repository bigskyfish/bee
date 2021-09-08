package com.floatcloud.beefz.base.util;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import com.floatcloud.beefz.base.pojo.BaseServerConfigPojo;
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
public class SftpUtils {


    private String charset = Charset.defaultCharset().toString();
    private static final int TIME_OUT = 1000 * 10 * 60;

    private Connection conn;


    /**
     * 登录指远程服务器
     * @return
     * @throws IOException
     */
    private boolean login(BaseServerConfigPojo serverConfigPojo) throws IOException {
        conn = new Connection(serverConfigPojo.getIp());
        conn.connect();
        return conn.authenticateWithPassword(serverConfigPojo.getUser(), serverConfigPojo.getPassword());
    }

    public int exec(BaseServerConfigPojo serverConfigPojo, String shell) {
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

    public String execForBack(BaseServerConfigPojo serverConfigPojo, String shell) {
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

}
