package com.floatcloud.beefz.controller;

import com.floatcloud.beefz.base.service.BaseService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;

/**
 * @author floatcloud
 */
@Controller
public class BaseServerController {

    @Resource
    private BaseService baseService;

    @GetMapping(value = "/v1/meson/install")
    public String installMeson(@RequestParam(defaultValue = "/root/meson/meson.csv") String filePath){
        baseService.installMeson(filePath);
        return null;
    }
}
