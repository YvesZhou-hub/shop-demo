package com.shop.demo.mapper;

import com.shop.demo.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    // 根据用户名查询用户
    User selectByUsername(String username);
}