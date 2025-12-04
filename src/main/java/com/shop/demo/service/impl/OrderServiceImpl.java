package com.shop.demo.service.impl;

import com.shop.demo.entity.Order;
import com.shop.demo.entity.Product;
import com.shop.demo.exception.InsufficientStockException;
import com.shop.demo.exception.ProductNotFoundException;
import com.shop.demo.mapper.OrderMapper;
import com.shop.demo.mapper.ProductMapper;
import com.shop.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单服务实现类（修复事务一致性及库存问题）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper; // 新增库存操作依赖

    /**
     * 新增订单（包含事务控制、库存校验与扣减）
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 关键：事务控制，确保原子性
    public int addOrder(Order order) {
        // 1. 基础参数校验
        if (order == null) {
            log.warn("新增订单失败：订单信息为空");
            return 0;
        }
        if (order.getUserId() == null || order.getUserId() <= 0) {
            log.warn("新增订单失败：用户 ID 非法（userId={}）", order.getUserId());
            return 0;
        }
        if (order.getProductId() == null || order.getProductId() <= 0) {
            log.warn("新增订单失败：商品 ID 非法（productId={}）", order.getProductId());
            return 0;
        }
        if (order.getNum() == null || order.getNum() <= 0) {
            log.warn("新增订单失败：购买数量非法（num={}）", order.getNum());
            return 0;
        }

        // 2. 查询商品（加锁防止并发问题）
        Product product = productMapper.selectByIdForUpdate(order.getProductId()); // 悲观锁查询
        if (product == null) {
            throw new ProductNotFoundException("商品不存在（productId=" + order.getProductId() + "）");
        }

        // 3. 库存校验
        if (product.getStock() == null || product.getStock() < order.getNum()) {
            throw new InsufficientStockException(
                    "库存不足（商品ID=" + order.getProductId() +
                            ", 库存=" + product.getStock() +
                            ", 购买数量=" + order.getNum()
            );
        }

        // 4. 计算订单总价（防止前端篡改，用商品单价重新计算）
        BigDecimal actualTotalPrice = product.getPrice().multiply(BigDecimal.valueOf(order.getNum()));
        order.setTotalPrice(actualTotalPrice);

        // 5. 扣减库存
        int newStock = product.getStock() - order.getNum();
        int updateResult = productMapper.updateStock(
                order.getProductId(),
                newStock,
                order.getNum() // 传递购买数量用于SQL条件判断
        );
        if (updateResult <= 0) {
            log.error("库存扣减失败，可能存在并发冲突（productId={}）", order.getProductId());
            throw new RuntimeException("库存扣减失败，请重试");
        }

        // 6. 创建订单
        order.setCreateTime(LocalDateTime.now());
        int result = orderMapper.insertOrder(order);
        log.info("订单创建成功：订单ID={}, 商品库存已更新（原库存={}, 新库存={}",
                order.getId(), product.getStock(), newStock);
        return result;
    }

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            log.warn("查询订单失败：用户 ID 非法（userId={}）", userId);
            return List.of();
        }
        log.info("开始查询用户 ID={} 的所有订单", userId);
        List<Order> orders = orderMapper.selectByUserId(userId);
        return Optional.ofNullable(orders).orElse(List.of());
    }
}