package com.floatcloud.beefz.util;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import com.floatcloud.beefz.pojo.ServerConfigPojo;
import lombok.Data;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author floatcloud
 */
@Data
public class SshClientUtil {


    private String charset = Charset.defaultCharset().toString();
    private static final int TIME_OUT = 1000 * 5 * 60;

    private static Connection conn;


    /**
     * 登录指远程服务器
     * @return
     * @throws IOException
     */
    private static boolean login(ServerConfigPojo serverConfigPojo) throws IOException {
        conn = new Connection(serverConfigPojo.getIp());
        conn.connect();
        return conn.authenticateWithPassword(serverConfigPojo.getUser(), serverConfigPojo.getPassword());
    }

    public static int exec(ServerConfigPojo serverConfigPojo, String shell) {
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


    public static void main(String[] args) {
        String shell = "chmod 777 /mnt/bee/setup.sh && sh /mnt/bee/setup.sh https://github.com/ethersphere/bee-clef/releases/download/v0.4.12/bee-clef_0.4.12_amd64.rpm bee-clef_0.4.12_amd64.rpm https://github.com/ethersphere/bee/releases/download/v0.5.3/bee_0.5.3_386.rpm bee_0.5.3_386.rpm  -d 1 -p 9ol.0p;/";
        SshClientUtil.exec(new ServerConfigPojo("47.98.53.84", "root", "9ol.0p;/", 22), shell);
    }
}
