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

    private ServerConfigPojo serverConfigPojo;

    private String charset = Charset.defaultCharset().toString();
    private static final int TIME_OUT = 1000 * 5 * 60;

    private Connection conn;

    public SshClientUtil(ServerConfigPojo serverConfigPojo) {
        this.serverConfigPojo = serverConfigPojo;
    }

    /**
     * 登录指远程服务器
     * @return
     * @throws IOException
     */
    private boolean login() throws IOException {
        conn = new Connection(this.serverConfigPojo.getIp());
        conn.connect();
        return conn.authenticateWithPassword(this.serverConfigPojo.getUser(), this.serverConfigPojo.getPassword());
    }

    public int exec(String shell) throws Exception {
        int ret = -1;
        try {
            if (login()) {
                Session session = conn.openSession();
                session.execCommand(shell);
                session.waitForCondition(ChannelCondition.EXIT_STATUS, TIME_OUT);
                ret = session.getExitStatus();
            } else {
                // 自定义异常类 实现略
                throw new RuntimeException("登录远程机器失败" + this.serverConfigPojo.getIp());
            }
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        return ret;
    }

    public static void main(){
        try {
            SshClientUtil sshClient = new SshClientUtil(new ServerConfigPojo("", "username", "password"));
            sshClient.exec("服务器shell脚本路径");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
