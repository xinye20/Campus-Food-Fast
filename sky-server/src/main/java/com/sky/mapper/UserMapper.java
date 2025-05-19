package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 添加新用户
     * @param user
     */
    void insert(User user);

    /**
     * 获取指定日期用户数量
     * @param map
     * @return
     */
    Integer countUserByMap(Map map);
}
