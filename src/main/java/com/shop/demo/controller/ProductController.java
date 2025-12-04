package com.shop.demo.controller;

import com.shop.demo.entity.Product;
import com.shop.demo.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品接口控制器（适配 Spring Boot 3.x + JDK 17）
 */
@Slf4j
@RestController
@RequestMapping("/product") // 接口前缀：/product
@RequiredArgsConstructor // 构造函数注入（和你OrderController风格一致）
@Validated
public class ProductController {

    private final ProductService productService; // 注入商品服务

    /**
     * 查询所有商品（GET请求：/product/all）
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllProducts() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Product> products = productService.getAllProducts();
            response.put("code", 200);
            response.put("msg", "查询成功");
            response.put("data", products);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询所有商品异常", e);
            response.put("code", 500);
            response.put("msg", "服务器内部错误");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 根据ID查询商品（GET请求：/product/{id}）
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProductById(
            @PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Product product = productService.getProductById(id);
            if (product != null) {
                response.put("code", 200);
                response.put("msg", "查询成功");
                response.put("data", product);
            } else {
                response.put("code", 404);
                response.put("msg", "商品不存在");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询商品详情异常", e);
            response.put("code", 500);
            response.put("msg", "服务器内部错误");
            return ResponseEntity.status(500).body(response);
        }
    }
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addProduct(@Valid @RequestBody Product product) {
        Map<String, Object> response = new HashMap<>();
        try {
            int result = productService.addProduct(product);
            if (result > 0) {
                response.put("code", 200);
                response.put("msg", "商品添加成功");
                response.put("data", product.getId());
                return ResponseEntity.ok(response);
            } else {
                response.put("code", 400);
                response.put("msg", "商品添加失败");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("添加商品异常", e);
            response.put("code", 500);
            response.put("msg", "服务器内部错误");
            return ResponseEntity.status(500).body(response);
        }
    }
}