package com.shop.demo.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Product {
    private Long id;                // 商品ID（对应表中id）
    private String productName;     // 商品名称（对应表中product_name）
    private BigDecimal price;       // 商品价格（对应表中price）
    private Integer stock;          // 商品库存（对应表中stock）
    private String description;     // 商品描述（对应表中description）
    private LocalDateTime createTime; // 创建时间（对应表中create_time）
}