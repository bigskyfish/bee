package com.floatcloud.beefz.mapper;

import com.floatcloud.beefz.base.pojo.BaseServerConfigPojo;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author floatcloud
 */
@Mapper
public interface BaseServerMapper {

    /**
     * 服务表添加数据
     * @param baseServerConfigPojo 服务信息
     */
    void addServer(BaseServerConfigPojo baseServerConfigPojo);
}
