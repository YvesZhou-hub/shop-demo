package com.shop.demo.controller;

import com.shop.demo.entity.Order;
import com.shop.demo.exception.InsufficientStockException;
import com.shop.demo.exception.ProductNotFoundException;
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

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addOrder(@Valid @RequestBody Order order) {
        Map<String, Object> response = new HashMap<>();
        try {
            int result = orderService.addOrder(order);
            if (result > 0) {
                response.put("code", 200);
                response.put("msg", "订单创建成功");
                response.put("data", order.getId());
                return ResponseEntity.ok(response);
            }
        } catch (ProductNotFoundException | InsufficientStockException e) {
            // 捕获业务异常，返回具体错误信息
            response.put("code", 400);
            response.put("msg", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("创建订单接口异常", e);
            response.put("code", 500);
            response.put("msg", "服务器内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        response.put("code", 400);
        response.put("msg", "订单创建失败");
        return ResponseEntity.badRequest().body(response);
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