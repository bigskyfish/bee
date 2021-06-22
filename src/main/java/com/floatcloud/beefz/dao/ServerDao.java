package com.floatcloud.beefz.dao;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ServerDao {
    int deleteByPrimaryKey(Integer id);

    int insert(Server record);

    List<Server> selectServersByStatus(Integer status);

    List<Server> selectNeedConnectServers();

    List<Server> selectServersByIp(String ip);

    List<Server> selectServersByIpList(List<String> ips);

    List<Server> selectServers();

    int updateStatus(String ip, int status);

    int insertSelective(Server record);

    Server selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Server record);

    int updateByPrimaryKey(Server record);
}