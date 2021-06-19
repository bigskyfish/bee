package com.floatcloud.beefz.controller.v2;

import com.floatcloud.beefz.pojo.BeeVersionPojo;
import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.floatcloud.beefz.util.FileEditUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author aiyuner
 */
@RestController
@RequestMapping("/api/v2")
public class BeeController {


    @GetMapping("/import/server")
    public void importServer(){
        List<ServerConfigPojo> servers = FileEditUtil.getServers();

    }



    @GetMapping("/bee/setup")
    public String sendFile(@RequestParam(name="beeversion") String beeVersion,
                           @RequestParam(name="beerpm") String beeRpm,
                           @RequestParam(name="begin", defaultValue = "1") int begin,
                           @RequestParam(name="gas", defaultValue = "10000") int gas) {
        BeeVersionPojo beeVersionPojo = new BeeVersionPojo();
        beeVersionPojo.setBeginIndex(begin);
        beeVersionPojo.setBeeVersion(beeVersion);
        beeVersionPojo.setBeeRpm(beeRpm);
        // sendFileService.sendFileToRemote(FileEditUtil.getServers(), beeVersionPojo, remove, gas);
        return "发送成功";
    }




}
