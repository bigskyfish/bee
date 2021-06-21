package com.floatcloud.beefz.dao;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NodeDao {
    int deleteByPrimaryKey(Integer id);

    int insert(Node record);

    int insertSelective(Node record);

    Node selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Node record);

    int updateByPrimaryKey(Node record);

    int updateByIpAndIdSelective(Node record);

    int updateAddressByIpAndId(String ip, int id, String address);

    int updateStatusByIpAndIds(String ip, List<String> ids);

    int updateStatusByIp(String ip);

    List<Node> selectAll();
}