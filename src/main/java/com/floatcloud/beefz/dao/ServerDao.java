package com.floatcloud.beefz.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ServerDao {
    int deleteByPrimaryKey(Integer id);

    int insert(Server record);

    @Select("select * from server where status = #{status, jdbcType=INTEGER}")
    List<Server> selectServersByStatus(int status);

    @Select("select * from server order by status")
    List<Server> selectServers();

    @Update("update server set status = #{status, jdbcType=INTEGER} where ip = #{ip, jdbcType=VARCHAR}")
    int updateStatus(String ip, int status);

    int insertSelective(Server record);

    Server selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Server record);

    int updateByPrimaryKey(Server record);
}