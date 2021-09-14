package com.floatcloud.beefz.base.service;

import com.floatcloud.beefz.base.constant.BaseConstant;
import com.floatcloud.beefz.base.pojo.BaseServerConfigPojo;
import com.floatcloud.beefz.base.util.BaseSftpUtils;
import com.floatcloud.beefz.base.util.SftpUtils;
import com.floatcloud.beefz.base.util.TransferFileUtils;
import com.floatcloud.beefz.mapper.BaseServerMapper;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

/**
 * 基础服务
 * @author floatcloud
 */
@Service
@Slf4j
public class BaseService {

    @Resource
    private BaseServerMapper baseServerMapper;
    /**
     * 远程安装文件地址
     */
    private static final String INSTALL_FILE_PATH = "/root/meson/";


    public static final String INSTALL_SHELL = "nohup sh /root/meson/mesonInstall.sh  >/root/meson/meson.log 2>&1 & ";


    public static final String OPEN_PORT_SHELL = "nohup sh /root/meson/openPort.sh >/root/meson/port.log 2>&1 & ";


    private final ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(16, 300, 30,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(500), new NamedThreadFactory("bee"));

    /**
     * 安装 Meson
     * @param filePath 服务器文件路径
     */
    public void installMeson(String filePath){
        List<BaseServerConfigPojo> servers = TransferFileUtils.transferFileToServerConfig(filePath);
        List<String> fileNames = new ArrayList<>();
        fileNames.add("openPort.sh");
        fileNames.add("LinuxVMDiskAutoInitialize.sh");
        fileNames.add("mesonInstall.sh");
        if(!servers.isEmpty()){
            BaseSftpUtils.clearUp(BaseConstant.DESCRIBE_PATH + "error.txt");
            for (BaseServerConfigPojo baseServerConfigPojo : servers){
                log.info("======执行安装meson：ip为 {} ======", baseServerConfigPojo.getIp());
                if (!poolExecutor.isShutdown()) {
                    poolExecutor.execute(() -> {
                        BaseSftpUtils sftpUtils = new BaseSftpUtils(baseServerConfigPojo);
                        SftpUtils sftp = new SftpUtils();
                        sftpUtils.sendFileToRemote(INSTALL_FILE_PATH, BaseConstant.DESCRIBE_PATH, fileNames);
                        sftp.exec(baseServerConfigPojo, INSTALL_SHELL);
                    });
                }
            }
        }
    }

    public void openPort(String filePath){
        List<BaseServerConfigPojo> servers = TransferFileUtils.transferFileToServerConfig(filePath);
        if(!servers.isEmpty()){
            for (BaseServerConfigPojo baseServerConfigPojo : servers){
                log.info("======执行开放端口：ip为 {} ======", baseServerConfigPojo.getIp());
                if (!poolExecutor.isShutdown()) {
                    poolExecutor.execute(() -> {
                        SftpUtils sftp = new SftpUtils();
                        sftp.exec(baseServerConfigPojo, OPEN_PORT_SHELL);
                    });
                }
            }
        }
    }



    public void shell(String filePath, String shell){
        List<BaseServerConfigPojo> servers = TransferFileUtils.transferFileToServerConfig(filePath);
        if(!servers.isEmpty()){
            for (BaseServerConfigPojo baseServerConfigPojo : servers){
                log.info("======执行执行脚本：ip为 {} ======", baseServerConfigPojo.getIp());
                if (!poolExecutor.isShutdown()) {
                    poolExecutor.execute(() -> {
                        SftpUtils sftp = new SftpUtils();
                        sftp.exec(baseServerConfigPojo, shell);
                    });
                }
            }
        }
    }


    public void sendFiles(String filePath, String fileNames){
        List<BaseServerConfigPojo> servers = TransferFileUtils.transferFileToServerConfig(filePath);
        String[] fileNameArr = fileNames.split(",");
        List<String> files = Arrays.asList(fileNameArr);
        if(!servers.isEmpty()){
            for (BaseServerConfigPojo baseServerConfigPojo : servers){
                log.info("======执行传送文件：ip为 {} ======", baseServerConfigPojo.getIp());
                if (!poolExecutor.isShutdown()) {
                    poolExecutor.execute(() -> {
                        BaseSftpUtils sftpUtils = new BaseSftpUtils(baseServerConfigPojo);
                        sftpUtils.sendFileToRemote(INSTALL_FILE_PATH, BaseConstant.DESCRIBE_PATH, files);
                    });
                }
            }
        }
    }
}
