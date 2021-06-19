package com.floatcloud.beefz.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NodeDao {
    int deleteByPrimaryKey(Integer id);

    int insert(Node record);

    int insertSelective(Node record);

    Node selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Node record);

    int updateByPrimaryKey(Node record);

    @Update("update node set node_address=#{address, jdbcType=VARCHAR} where node_ip=#{ip, jdbcType=VARCHAR} and node_id=#{id, jdbcType=INTEGER}")
    int updateAddressByIpAndId(String ip, int id, String address);
}