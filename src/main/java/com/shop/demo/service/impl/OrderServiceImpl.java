package com.shop.demo.service.impl;

import com.shop.demo.entity.Order;
import com.shop.demo.entity.Product;
import com.shop.demo.mapper.OrderMapper;
import com.shop.demo.mapper.ProductMapper;
import com.shop.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 订单服务实现类（适配 Spring Boot 3.x + JDK 17）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    // 构造函数注入（Spring 3.x 推荐）
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper; // 注入商品Mapper用于扣减库存

    /**
     * 新增订单（包含库存扣减和事务控制）
     */
    @Override
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.DEFAULT,
            rollbackFor = Exception.class
    )
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
        if (order.getTotalPrice() == null || order.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("新增订单失败：订单总价非法（totalPrice={}）", order.getTotalPrice());
            return 0;
        }

        // 2. 日志打印（便于追踪订单创建）
        log.info("开始创建订单：userId={}, productId={}, 购买数量={}, 总价={}",
                order.getUserId(), order.getProductId(), order.getNum(), order.getTotalPrice());

        // 3. 查询商品库存是否充足
        Product product = productMapper.selectById(order.getProductId());
        if (product == null) {
            log.warn("商品不存在：productId={}", order.getProductId());
            return 0;
        }
        if (product.getStock() < order.getNum()) {
            log.warn("库存不足：productId={}, 需求={}, 库存={}",
                    order.getProductId(), order.getNum(), product.getStock());
            return 0;
        }

        // 4. 扣减库存
        int newStock = product.getStock() - order.getNum();
        int updateCount = productMapper.updateStock(order.getProductId(), newStock);
        if (updateCount == 0) {
            // 库存更新失败（可能被其他线程抢先扣减），触发事务回滚
            throw new RuntimeException("库存扣减失败，可能已被其他订单占用");
        }

        // 5. 执行数据库插入订单
        int insertCount = orderMapper.insertOrder(order);
        if (insertCount == 0) {
            // 订单插入失败，触发事务回滚（库存会自动回滚）
            throw new RuntimeException("订单创建失败");
        }

        log.info("订单创建成功：订单 ID={}", order.getId());
        return insertCount;
    }

    /**
     * 根据用户 ID 查询订单
     */
    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        // 1. 参数校验
        if (userId == null || userId <= 0) {
            log.warn("查询订单失败：用户 ID 非法（userId={}）", userId);
            return List.of(); // 非法 ID 返回空集合，避免 null
        }

        // 2. 日志打印
        log.info("开始查询用户 ID={} 的所有订单", userId);

        // 3. 执行查询（空结果返回空集合）
        List<Order> orders = orderMapper.selectByUserId(userId);
        return Optional.ofNullable(orders).orElse(List.of());
    }
}