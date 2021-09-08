package com.floatcloud.beefz.base.service;

import com.floatcloud.beefz.base.pojo.BaseServerConfigPojo;
import com.floatcloud.beefz.base.util.BaseSftpUtils;
import com.floatcloud.beefz.base.util.TransferFileUtils;
import com.floatcloud.beefz.mapper.BaseServerMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

/**
 * 基础服务
 * @author floatcloud
 */
@Service
public class BaseService {

    @Resource
    private BaseServerMapper baseServerMapper;
    /**
     * 远程安装文件地址
     */
    private static final String INSTALL_FILE_PATH = "/root/meson/";


    public static final String INSTALL_SHELL = "nohup sh /root/meson/mesonInstall.sh  >/root/meson/meson.log 2>&1 & ";

    /**
     * 安装 Meson
     * @param filePath 服务器文件路径
     */
    public void installMeson(String filePath){
        List<BaseServerConfigPojo> servers = TransferFileUtils.transferFileToServerConfig(filePath);
        List<String> fileNames = new ArrayList<>();
        fileNames.add("mesonInstall.sh");
        if(!servers.isEmpty()){
            for (BaseServerConfigPojo baseServerConfigPojo : servers){
                // baseServerMapper.addServer(baseServerConfigPojo);
                // 执行上传脚本并执行
                BaseSftpUtils sftpUtils = new BaseSftpUtils(baseServerConfigPojo);
                sftpUtils.sendFileToRemote(INSTALL_FILE_PATH, "/Users/floatcloud/Miner/beeServer/beefz/src/main/java/describe/", fileNames);
                sftpUtils.exec(INSTALL_SHELL);
            }
        }
    }

}
