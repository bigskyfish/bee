package com.floatcloud.beefz.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * @author floatcloud
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ServerConfigPojo {

    private String ip;
    private String user;
    private String password;
    private Integer port;
    // bee 的 Swarm项目地址
    private String endPoint;
    // 节点数
    private Integer nodeNum;
    // 失败节点
    private List<Integer> errorNode;

}
