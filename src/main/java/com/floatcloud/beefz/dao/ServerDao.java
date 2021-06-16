package com.floatcloud.beefz.dao;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ServerDao {
    int deleteByPrimaryKey(Integer id);

    int insert(Server record);

    int insertSelective(Server record);

    Server selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Server record);

    int updateByPrimaryKey(Server record);
}