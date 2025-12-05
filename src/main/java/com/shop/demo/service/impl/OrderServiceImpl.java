package com.shop.demo.service.impl;

import com.shop.demo.dto.BatchOrderRequest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;

    /**
     * 单个下单接口
     * 兼容旧的单个商品下单逻辑，直接调用核心处理方法
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addOrder(Order order) {
        return processSingleOrder(order);
    }

    /**
     * 批量下单接口：原子性事务
     * 循环处理每一个商品，只要有一个失败（库存不足等），整个方法抛出异常，事务全部回滚
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> createBatchOrder(BatchOrderRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("订单列表不能为空");
        }

        log.info("开始处理用户 {} 的批量订单，包含 {} 个商品", request.getUserId(), request.getItems().size());

        List<Long> createdOrderIds = new ArrayList<>();

        // 遍历请求中的每一个商品项
        for (BatchOrderRequest.OrderItemRequest item : request.getItems()) {
            Order order = new Order();
            order.setUserId(request.getUserId());
            order.setProductId(item.getProductId());
            order.setNum(item.getNum());

            // 调用核心逻辑（复用代码）
            processSingleOrder(order);

            // 收集生成的订单 ID
            createdOrderIds.add(order.getId());
        }

        return createdOrderIds;
    }

    /**
     * 核心下单逻辑（提取公用方法）
     * 包含：参数校验 -> 悲观锁查询 -> 库存校验 -> 价格计算 -> 扣库存 -> 插入订单
     */
    private int processSingleOrder(Order order) {
        // 0. 基础防守校验
        if (order.getUserId() == null || order.getProductId() == null || order.getNum() == null || order.getNum() <= 0) {
            throw new IllegalArgumentException("订单参数不完整");
        }

        // 1. 锁查询 (SELECT ... FOR UPDATE)
        // 这一步会锁住该商品的数据库行，直到事务结束。防止并发超卖。
        Product product = productMapper.selectByIdForUpdate(order.getProductId());
        if (product == null) {
            throw new ProductNotFoundException("商品不存在（productId=" + order.getProductId() + "）");
        }

        // 2. 库存检查
        if (product.getStock() == null || product.getStock() < order.getNum()) {
            throw new InsufficientStockException("商品 [" + product.getProductName() + "] 库存不足，当前库存：" + product.getStock());
        }

        // 3. 计算价格 (后端计算，防止前端篡改)
        // 使用 BigDecimal 进行精确计算
        BigDecimal actualTotalPrice = product.getPrice().multiply(BigDecimal.valueOf(order.getNum()));
        order.setTotalPrice(actualTotalPrice);

        // 4. 扣减库存 (双重保险：SQL 中再次判断 stock >= num)
        int newStock = product.getStock() - order.getNum();
        int updateResult = productMapper.updateStock(
                order.getProductId(),
                newStock,
                order.getNum() // 传递购买数量用于 SQL 条件判断
        );

        if (updateResult <= 0) {
            // 如果 update 返回 0，说明在锁获取之后到更新之间数据发生了意料之外的变化，或者 SQL 条件未满足
            log.error("库存扣减失败，并发冲突（productId={}）", order.getProductId());
            throw new RuntimeException("系统繁忙，请重试");
        }

        // 5. 插入订单
        order.setCreateTime(LocalDateTime.now());
        int result = orderMapper.insertOrder(order);

        log.info("下单成功：订单ID={}, 商品={}, 数量={}", order.getId(), product.getProductName(), order.getNum());
        return result;
    }

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        List<Order> orders = orderMapper.selectByUserId(userId);
        return Optional.ofNullable(orders).orElse(List.of());
    }
}