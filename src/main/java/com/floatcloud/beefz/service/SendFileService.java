package com.floatcloud.beefz.service;

import com.floatcloud.beefz.constant.FileConstant;
import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.floatcloud.beefz.util.FileDistributeUtil;
import com.floatcloud.beefz.util.FileEditUtil;
import com.floatcloud.beefz.util.SshClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author floatcloud
 */
@Service
@Slf4j
public class SendFileService {

    public static final String SET_UP_PATH = "/mnt/bee/setup.sh";

    public static final String SET_UP_SH = "bee start --config /mnt/bee/bee-config.yaml";

    private ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(25, 100, 5,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(500));

    public void sendFileToRemote(List<ServerConfigPojo> serverList) throws FileNotFoundException {
        try {
            String srcPath = System.getProperty("user.dir") + "/src/";
            FileInputStream setup = new FileInputStream(srcPath + FileConstant.SET_UP_SH);
            if (serverList != null && !serverList.isEmpty()) {
                // 执行 派发动作
                String password = "\r\npassword: ";
                final String path = " > /mnt/bee/privateKey.key";
                final String setUpName = "setup.sh";
                serverList.forEach(serverConfigPojo -> {
                    // 这里有待商榷
                    CountDownLatch countDownLatch = new CountDownLatch(1);
                    String beeConfig = FileEditUtil.appendMsg(srcPath + FileConstant.BEE_CONFIG,
                            password + serverConfigPojo.getPassword());
                    String transferKey = FileEditUtil.appendMsg(srcPath + FileConstant.TRANSFER_PRIVATE_KEY_NAME,
                            " " + serverConfigPojo.getPassword() + path);
                    try {
                        FileDistributeUtil.uploadFile(serverConfigPojo, beeConfig, FileConstant.BEE_CONFIG);
                        FileDistributeUtil.uploadFile(serverConfigPojo, transferKey, FileConstant.TRANSFER_PRIVATE_KEY_NAME);
                        FileDistributeUtil.uploadFile(serverConfigPojo, setup, setUpName);
                    } catch (Exception e) {
                        log.error("====FileDistributeUtil 上传 error====", e);
                    } finally {
                        countDownLatch.countDown();
                    }
                    try {
                        countDownLatch.await();
                    } catch (InterruptedException e) {
                        log.error("====countDownLatch error====", e);
                    }
                    poolExecutor.execute(() -> {
                        try {
                            // 执行矿机启动脚本
                            log.info("=======启动一键挖矿脚本=======");
                            String shStr = "chmod 777 "+ SET_UP_PATH +" && sh " + SET_UP_PATH;
                            int exec = SshClientUtil.exec(serverConfigPojo, shStr);
                            log.info("======= 启动一键挖矿脚本 执行结果: " + exec +"=======");
                            if (exec != -1){
                                int beeStart = SshClientUtil.exec(serverConfigPojo, SET_UP_SH);
                                log.info("====== 挖矿程序启动 ===" + beeStart);
                            }
                        } catch (Exception e) {
                            log.error("====SshClientUtil 执行脚本 error====", e);
                        }
                    });
                });
            }
        } finally {
            poolExecutor.shutdown();
        }
    }

}
