package com.floatcloud.beefz.sysenum;

import lombok.ToString;

/**
 * @author floatcloud
 */

@ToString
public enum ServerStatusEnum {
    UN_INSTANCE("未完成安装"), INSTANCE("已安装"), RUNNING("运行中"),CLOSED("停止");

    private String type;

    ServerStatusEnum(String type) {
        this.type = type;
    }
    public String getType(){
        return type;
    }
}
