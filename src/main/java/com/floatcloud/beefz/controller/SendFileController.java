package com.floatcloud.beefz.controller;

import com.floatcloud.beefz.pojo.BeeVersionPojo;
import com.floatcloud.beefz.service.SendFileService;
import com.floatcloud.beefz.util.FileEditUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

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
        return "发送成功";
    }

    @GetMapping("/private/keys")
    public ModelAndView getPrivateKeys(){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("servers", sendFileService.getPrivateKey(FileEditUtil.getServers()));
        modelAndView.setViewName("beeServer");
        return modelAndView;
    }

    @GetMapping("/bee/restart")
    public String restartBee(){
        sendFileService.restartBeeServer(FileEditUtil.getServers());
        return "重启成功！";
    }

}
