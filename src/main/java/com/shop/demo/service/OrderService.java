package com.shop.demo.service;

import com.shop.demo.dto.BatchOrderRequest;
import com.shop.demo.entity.Order;
import java.util.List;

public interface OrderService {
    // 单个下单
    int addOrder(Order order);

    //  批量下单接口
    List<Long> createBatchOrder(BatchOrderRequest request);

    List<Order> getOrdersByUserId(Long userId);
}