package com.floatcloud.beefz.dao;

import lombok.Data;

import java.io.Serializable;

/**
 * server
 * @author 
 */
@Data
public class Server implements Serializable {

    private int id;

    /**
     * ip地址
     */
    private String ip;

    /**
     * 账户
     */
    private String user;

    /**
     * 密码
     */
    private String psd;

    /**
     * 端口
     */
    private Integer port;

    private String endpoint;

    /**
     * 节点数
     */
    private Integer nodeNum;

    /**
     * cpu核数
     */
    private Integer cpuNum;

    /**
     * 存储
     */
    private String memory;

    /**
     * 带宽
     */
    private String bandWidth;

    /**
     * 其他描述信息
     */
    private String detail;

    private String cpuUsed;

    private String memoryUsed;

    private String allDisk;

    private String diskUsed;

    /**
     * 服务器联通情况（检查网络）0：未连通（ping不同）1：已连通
     */
    private Integer status;
}