package com.floatcloud.beefz.dao;

import lombok.Data;

import java.io.Serializable;

/**
 * node
 * @author 
 */
@Data
public class Node implements Serializable {
    /**
     * 主键
     */
    private Integer id;

    /**
     * 节点所在ip
     */
    private String nodeIp;

    /**
     * 节点所在编号
     */
    private Integer nodeId;

    /**
     * 节点名称（对应包名）
     */
    private String nodeName;

    /**
     * 节点状态0：默认未运行
     */
    private Integer nodeStatus;

    /**
     * 节点地址
     */
    private String nodeAddress;

    /**
     * 节点私钥
     */
    private String nodePrivateKey;

    /**
     * 出票情况
     */
    private Integer nodeTicketNum;

    /**
     * 节点的详细信息
     */
    private String nodeDetail;

    private static final long serialVersionUID = 1L;
}