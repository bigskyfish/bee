package com.floatcloud.beefz.controller.v2;

import com.floatcloud.beefz.dao.Server;
import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.floatcloud.beefz.service.v2.BeeService;
import com.floatcloud.beefz.util.FileEditUtil;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

import javax.annotation.Resource;

/**
 * @author aiyuner
 */
@RestController
@RequestMapping("/api/v2")
public class BeeController {

    @Resource
    private BeeService beeService;


    @GetMapping("/import/server")
    public void importServer(@RequestParam(required = false) String path){
        List<ServerConfigPojo> servers;
        if(path == null){
            servers = FileEditUtil.getServers();
        } else {
            servers = FileEditUtil.getServerList(new File(path));
        }
        beeService.insertServers(servers);
    }

    @GetMapping("/servers")
    public List<Server> importServer(@RequestParam(required = false) Integer status,
                                     @RequestParam(defaultValue = "0") int show){
        List<Server> servers = beeService.getServers(status);
        if(show == 0){
            servers.forEach(server -> {
                server.setUser("******");
                server.setPsd("******");
            });
        }
        return servers;
    }

    @GetMapping("/bee/setup")
    public String beeSetup(@RequestParam(defaultValue = "") String version) {
        List<Server> servers = beeService.getServers(-1);
        beeService.beeSetup(servers, version);
        return "发送成功";
    }


    @PostMapping("/address")
    public void getAddress(@RequestParam String ip,
                             @RequestParam String address){
        beeService.updateBeeNode(ip, address);
    }




}
