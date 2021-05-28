package com.floatcloud.beefz.service;

import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.floatcloud.beefz.pojo.ServerCoreResponsePojo;
import com.floatcloud.beefz.util.FileDistributeUtil;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author floatcloud
 */
@Service
@Slf4j
public class SendFileService {

    public static final String SET_UP_PATH = "/mnt/bee/setup.sh";


    private final ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(25, 100, 5,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(500), new NamedThreadFactory("bee"));

    @Value("${beedata}")
    private String fileNameStr;


    /**
     * 不执行删除bee
     */
    public static final String SHELL_BEE_SET_UP = "chmod 777 /mnt/bee/setup.sh && sh /mnt/bee/setup.sh -p ";
    /**
     * 执行删除bee
     */
    public static final String SHELL_BEE_SET_UP_REMOVE = "chmod 777 /mnt/bee/setup.sh && sh /mnt/bee/setup.sh -d -p ";

    /**
     * 执行删除bee
     */
    public static final String GET_PRIVATE_KEY = "chmod 777 /mnt/bee/transferPrivateKey.sh && sh /mnt/bee/transferPrivateKey.sh";

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 发送文件到远程服务器
     * @param serverList 服务集合
     */
    public void sendFileToRemote(List<ServerConfigPojo> serverList, Integer remove) {
        String[] split = fileNameStr.split(",");
        List<String> filenames = Stream.of(split).collect(Collectors.toList());
        final String shell = remove == 1 ? SHELL_BEE_SET_UP_REMOVE : SHELL_BEE_SET_UP;
        if (serverList != null && !serverList.isEmpty()) {
            // 执行 派发动作
            serverList.forEach(serverConfigPojo -> {
                try {
                    if(!poolExecutor.isShutdown()) {
                        poolExecutor.execute(() -> {
                            try {
                                FileDistributeUtil.uploadFiles(serverConfigPojo, filenames, shell + serverConfigPojo.getPassword());
                            } catch (Exception e) {
                                log.error("====SshClientUtil 执行脚本 error====", e);
                            }
                        });
                    }
                } catch (Exception e) {
                    log.error("====FileDistributeUtil 上传 error====", e);
                }
            });
        }
    }


    /**
     * 获取秘钥
     * @param serverList 服务集合
     */
    public List<ServerCoreResponsePojo> getPrivateKey(List<ServerConfigPojo> serverList) {
        List<ServerCoreResponsePojo> result = new ArrayList<>(serverList.size()+1);
        if (serverList != null && !serverList.isEmpty()) {
            // 执行 派发动作
            serverList.forEach(serverConfigPojo -> {
                try {
                    if(!poolExecutor.isShutdown()) {
                        poolExecutor.execute(() -> {
                            ServerCoreResponsePojo serverCoreResponsePojo =
                                    FileDistributeUtil.getServerCoreResponsePojo(serverConfigPojo, GET_PRIVATE_KEY);
                            try {
                                lock.lock();
                                result.add(serverCoreResponsePojo);
                            } finally {
                                lock.unlock();
                            }
                        });
                    }
                } catch (Exception e) {
                    log.error("====FileDistributeUtil 上传 error====", e);
                }
            });
        }
        return result;
    }


}
