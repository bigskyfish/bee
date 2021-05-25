package com.floatcloud.beefz.controller;

import com.floatcloud.beefz.constant.FileConstant;
import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.floatcloud.beefz.util.FileDistributeUtil;
import com.floatcloud.beefz.util.FileEditUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * @author floatcloud
 */
@RestController
@RequestMapping("/v1/api")
public class SendFileController {


    @GetMapping("/sendFile")
    public String sendFile() throws IOException {
        String srcPath = System.getProperty("user.dir") + "/src/";
        List<ServerConfigPojo> serverList = FileEditUtil.getServerList(new File(srcPath + FileConstant.CSV_PATH));
        FileInputStream setup = new FileInputStream(srcPath + FileConstant.SET_UP_SH);
        if(serverList.size() > 0){
           // 执行 派发动作
            String password = "\r\npassword: ";
            final String path = " > /mnt/bee/privateKey.key";
            final String setUpName = "setup.sh";
            serverList.forEach(serverConfigPojo -> {
                String beeConfig = FileEditUtil.appendMsg(srcPath + FileConstant.BEE_CONFIG,
                        password + serverConfigPojo.getPassword());
                String transferKey = FileEditUtil.appendMsg(srcPath + FileConstant.TRANSFER_PRIVATE_KEY_NAME,
                        " " + serverConfigPojo.getPassword()  + path );
                try {
                    FileDistributeUtil.uploadFile(serverConfigPojo, beeConfig, FileConstant.BEE_CONFIG);
                    FileDistributeUtil.uploadFile(serverConfigPojo, transferKey, FileConstant.TRANSFER_PRIVATE_KEY_NAME);
                    FileDistributeUtil.uploadFile(serverConfigPojo, setup, setUpName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        return "";
    }

}
