package com.floatcloud.beefz.controller.v2;

import com.floatcloud.beefz.dao.Server;
import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.floatcloud.beefz.pojo.ServerCoreResponsePojo;
import com.floatcloud.beefz.service.v2.BeeService;
import com.floatcloud.beefz.util.FileEditUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

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
    public String beeSetup(@RequestParam(defaultValue = "ethersphere/bee:0.6.2") String version) {
        List<Server> servers = beeService.getServers(-1);
        beeService.beeSetup(servers, version);
        return "发送成功";
    }


    @GetMapping("/address")
    public ModelAndView getNodes(@RequestParam(defaultValue = "0") String execShell,
                                 @RequestParam(defaultValue = "1") String downLoad,
                                 @RequestParam(defaultValue = "1") String ethNum,
                                 @RequestParam(defaultValue = "2") String gBzzNum){
        ModelAndView modelAndView = new ModelAndView();
        if("1".equals(execShell)){
            // 获取服务器信息脚本启动
            beeService.setupGetAddress();
        }
        List<ServerCoreResponsePojo> privateKeyList = beeService.getAddress();
        modelAndView.addObject("servers", privateKeyList);
        modelAndView.setViewName("beeAddress");
        if ("1".endsWith(downLoad)){
            beeService.downLoadBeeAddress(privateKeyList, ethNum, gBzzNum);
        }
        return modelAndView;
    }


    @PostMapping("/address")
    public void getAddress(@RequestParam String ip,
                           @RequestParam String address){
        beeService.updateBeeNode(ip, address);
    }




}
