package com.shop.demo.service.impl;

import com.shop.demo.entity.Product;
import com.shop.demo.mapper.ProductMapper;
import com.shop.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    /**
     * 添加商品（添加事务注解、修复时间设置冲突、完善返回逻辑）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)  // 开启事务，异常时回滚
    public int addProduct(Product product) {
        // 增强参数校验：商品名称非空且长度限制、价格大于 0
        if (product == null) {
            log.warn("添加商品失败：商品对象为空");
            return 0;
        }
        if (product.getProductName() == null || product.getProductName().trim().isEmpty() || product.getProductName().length() > 100) {
            log.warn("添加商品失败：商品名称非法（名称={}）", product.getProductName());
            return 0;
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("添加商品失败：商品价格非法（价格={}）", product.getPrice());
            return 0;
        }
        // 库存默认值（避免库存为 null）
        if (product.getStock() == null) {
            product.setStock(0);
        }

        // 移除手动设置 createTime：数据库已配置 DEFAULT CURRENT_TIMESTAMP，且 XML 中已指定 CURRENT_TIMESTAMP
        // 避免实体类时间类型（Date/LocalDateTime）与数据库 datetime 类型映射冲突
        log.info("开始添加商品：{}，价格：{}，库存：{}", product.getProductName(), product.getPrice(), product.getStock());

        try {
            productMapper.insertProduct(product);
            log.info("商品添加成功，生成商品 ID：{}", product.getId());  // 能获取到数据库自增 ID（useGeneratedKeys=true）
            return 1;  // 返回 1 表示成功（与 XML 中 insert 实际影响行数一致）
        } catch (Exception e) {
            log.error("添加商品失败", e);
            return 0;  // 返回 0 表示失败
        }
    }
}