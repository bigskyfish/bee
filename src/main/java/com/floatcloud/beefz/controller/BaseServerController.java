package com.floatcloud.beefz.controller;

import com.floatcloud.beefz.base.service.BaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author floatcloud
 */
@RestController
public class BaseServerController {

    @Resource
    private BaseService baseService;

    @GetMapping(value = "/v1/meson/install")
    public String installMeson(@RequestParam(defaultValue = "/Users/floatcloud/Miner/beeServer/beefz/src/main/java/describe/meson.csv") String filePath){
        baseService.installMeson(filePath);
        return "success";
    }

    @GetMapping(value = "/v1/meson/open/port")
    public String openPort(@RequestParam(defaultValue = "/Users/floatcloud/Miner/beeServer/beefz/src/main/java/describe/meson.csv") String filePath){
        baseService.openPort(filePath);
        return "success";
    }

    @GetMapping(value = "/v1/meson/shell")
    public String shell(@RequestParam(defaultValue = "/Users/floatcloud/Miner/beeServer/beefz/src/main/java/describe/meson.csv") String filePath,
                        @RequestParam String shell){
        baseService.shell(filePath, shell);
        return "success";
    }


    @GetMapping(value = "/v1/meson/send")
    public String sendFiles(@RequestParam(defaultValue = "/Users/floatcloud/Miner/beeServer/beefz/src/main/java/describe/meson.csv") String filePath,
                        @RequestParam String fileNames){
        baseService.sendFiles(filePath, fileNames);
        return "success";
    }

}
