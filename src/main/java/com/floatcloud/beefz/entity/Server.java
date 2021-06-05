package com.floatcloud.beefz.entity;

import lombok.Data;

/**
 * @author floatcloud
 */
@Data
public class Server {

    private int id;
    private String ip;
    private String user;
    private String psd;
    private int port = 22;
    private int nodeNum = 1;
    private int cpuNum;
    private String memory;
    private String bandWidth;
    private String detail;
    private int status;

}
