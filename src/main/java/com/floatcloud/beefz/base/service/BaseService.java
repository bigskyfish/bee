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

    public boolean installMeson(String filePath){
        List<BaseServerConfigPojo> servers = TransferFileUtils.transferFileToServerConfig(filePath);
        List<String> fileNames = new ArrayList<>();
        fileNames.add("mesonInstall.sh");
        if(!servers.isEmpty()){
            for (BaseServerConfigPojo baseServerConfigPojo : servers){
                // baseServerMapper.addServer(baseServerConfigPojo);
                // 执行上传脚本并执行
                BaseSftpUtils sftpUtils = new BaseSftpUtils(baseServerConfigPojo);
                sftpUtils.sendFileToRemote(INSTALL_FILE_PATH, INSTALL_FILE_PATH, fileNames);
            }
        }
        return true;
    }

}
