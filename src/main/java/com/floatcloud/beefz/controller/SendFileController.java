package com.floatcloud.beefz.controller;

import com.floatcloud.beefz.constant.FileConstant;
import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.floatcloud.beefz.service.SendFileService;
import com.floatcloud.beefz.util.FileEditUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;

/**
 * @author floatcloud
 */
@RestController
@RequestMapping("/v1/api")
public class SendFileController {

    @Resource
    private SendFileService sendFileService;

    @GetMapping("/sendFile")
    public String sendFile() throws IOException {
        String srcPath = System.getProperty("user.dir") + "/src/";
        List<ServerConfigPojo> serverList = FileEditUtil.getServerList(new File(srcPath + FileConstant.CSV_PATH));
        sendFileService.sendFileToRemote(serverList);
        return "";
    }

}
