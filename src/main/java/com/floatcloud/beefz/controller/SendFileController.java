package com.floatcloud.beefz.controller;

import com.floatcloud.beefz.pojo.ServerCoreResponsePojo;
import com.floatcloud.beefz.service.SendFileService;
import com.floatcloud.beefz.util.FileEditUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



import javax.annotation.Resource;
import java.util.List;

/**
 * @author floatcloud
 */
@RestController
@RequestMapping("/v1/api")
public class SendFileController {

    @Resource
    private SendFileService sendFileService;

    @GetMapping("/sendFile")
    public String sendFile(@RequestParam(name="remove", defaultValue = "0") int remove) {
        sendFileService.sendFileToRemote(FileEditUtil.getServers(), remove);
        return "";
    }

    @GetMapping("/private/keys")
    public List<ServerCoreResponsePojo> getPrivateKeys(){
        return sendFileService.getPrivateKey(FileEditUtil.getServers());
    }

}
