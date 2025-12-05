package com.shop.demo.controller;

import com.shop.demo.dto.BatchOrderRequest;
import com.shop.demo.dto.OrderRequest;
import com.shop.demo.entity.Order;
import com.shop.demo.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    // 保留单个 add 接口兼容旧代码...

    /**
     * 批量下单接口
     */
    @PostMapping("/add/batch")
    public ResponseEntity<Map<String, Object>> addOrderBatch(@Valid @RequestBody BatchOrderRequest batchReq) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Long> orderIds = orderService.createBatchOrder(batchReq);

            response.put("code", 200);
            response.put("msg", "批量下单成功");
            response.put("data", orderIds); // 返回所有生成的订单 ID
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // 这里捕获所有异常（包括库存不足），事务已在 Service 层回滚
            log.error("批量下单异常", e);
            response.put("code", 400);
            response.put("msg", "下单失败：" + e.getMessage()); // 返回具体错误（如库存不足）
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getOrdersByUserId(
            @PathVariable @Min(value = 1, message = "用户ID必须大于0") Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Order> orders = orderService.getOrdersByUserId(userId);
            response.put("code", 200);
            response.put("msg", "查询成功");
            response.put("data", orders);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询用户订单接口异常", e);
            response.put("code", 500);
            response.put("msg", "服务器内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(jakarta.validation.ConstraintViolationException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 400);
        response.put("msg", e.getConstraintViolations().iterator().next().getMessage());
        return ResponseEntity.badRequest().body(response);
    }
}