package com.shop.demo.mapper;

import com.shop.demo.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface OrderMapper {
    // 新增订单
    int insertOrder(Order order);
    // 根据用户ID查询订单
    List<Order> selectByUserId(Long userId);
}