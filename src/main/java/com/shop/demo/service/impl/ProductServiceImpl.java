package com.shop.demo.service.impl;

import com.shop.demo.entity.Product;
import com.shop.demo.mapper.ProductMapper;
import com.shop.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * 商品服务实现类（适配 Spring Boot 3.x + JDK 17）
 */
@Slf4j  // 替代 @Log，JDK 17 推荐 SLF4J 日志规范
@Service
@RequiredArgsConstructor  // Lombok 自动注入构造函数（替代 @Resource，更符合 Spring 官方推荐）
public class ProductServiceImpl implements ProductService {

    // 构造函数注入（无 @Resource 注解，避免字段注入的耦合问题）
    private final ProductMapper productMapper;

    /**
     * 查询所有商品（新增日志打印、空集合防护）
     */
    @Override
    public List<Product> getAllProducts() {
        log.info("开始查询所有商品");
        List<Product> products = productMapper.selectAll();
        // 避免返回 null，空结果返回空集合（前端处理更友好）
        return Optional.ofNullable(products).orElse(List.of());
    }

    /**
     * 根据 ID 查询商品（新增参数校验、日志打印）
     */
    @Override
    public Product getProductById(Long id) {
        // 参数校验（JDK 17 可结合 jakarta.validation 做更复杂校验）
        if (id == null || id <= 0) {
            log.warn("查询商品失败：商品 ID 非法（id={}）", id);
            return null;
        }

        log.info("开始查询 ID 为 {} 的商品", id);
        return productMapper.selectById(id);
    }
}