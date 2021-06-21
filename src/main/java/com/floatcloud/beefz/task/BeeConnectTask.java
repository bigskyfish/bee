package com.floatcloud.beefz.task;

import com.floatcloud.beefz.service.v2.BeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;

/**
 *
 * @author floatcloud
 */
@Configuration
@EnableScheduling
@Slf4j
public class BeeConnectTask {


    @Resource
    private BeeService beeService;


    @Scheduled(cron = "0 */3 * * * ?")
    private void connectBootNode(){
        // 每 3 分钟 执行一次
        beeService.connectBootNode(null);
    }


    @Scheduled(cron = "0 */5 * * * ?")
    private void updateBeeStatus(){
        // 每 5 分钟 同步一次服务器状态
        beeService.listenBeeStatus();
    }
}
