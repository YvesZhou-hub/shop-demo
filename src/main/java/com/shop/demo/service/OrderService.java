package com.shop.demo.service;

import com.shop.demo.entity.Order;
import java.util.List;

public interface OrderService {
    int addOrder(Order order);
    List<Order> getOrdersByUserId(Long userId);
}