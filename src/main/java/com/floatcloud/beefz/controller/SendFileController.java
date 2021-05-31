package com.floatcloud.beefz.controller;

import com.floatcloud.beefz.pojo.BeeVersionPojo;
import com.floatcloud.beefz.pojo.ServerCoreResponsePojo;
import com.floatcloud.beefz.service.SendFileService;
import com.floatcloud.beefz.util.FileEditUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public String sendFile(@RequestParam(name="remove", defaultValue = "0") int remove,
                           @RequestParam(name="beeversion") String beeVersion,
                           @RequestParam(name="beerpm") String beeRpm) {
        BeeVersionPojo beeVersionPojo = new BeeVersionPojo();
        beeVersionPojo.setBeeVersion(beeVersion);
        beeVersionPojo.setBeeRpm(beeRpm);
        sendFileService.sendFileToRemote(FileEditUtil.getServers(), beeVersionPojo, remove);
        return "";
    }

    @GetMapping("/private/keys")
    public List<ServerCoreResponsePojo> getPrivateKeys(){
        return sendFileService.getPrivateKey(FileEditUtil.getServers());
    }

}
