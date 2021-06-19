package com.floatcloud.beefz.dao;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NodeDao {
    int deleteByPrimaryKey(Integer id);

    int insert(Node record);

    int insertSelective(Node record);

    Node selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Node record);

    int updateByPrimaryKey(Node record);
}