package com.shop.demo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ToString
public class Order implements Serializable { // 1. 实现 Serializable 接口

    // 2. 添加版本号，防止类修改后反序列化报错
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull(message = "用户 ID 不能为空")
    @Min(value = 1, message = "用户 ID 必须大于 0")
    private Long userId;

    @NotNull(message = "商品 ID 不能为空")
    @Min(value = 1, message = "商品 ID 必须大于 0")
    private Long productId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于 0")
    private Integer num;

    @NotNull(message = "订单总价不能为空")
    @Min(value = 1, message = "订单总价必须大于0")
    private BigDecimal totalPrice;

    // 3. 添加 JSON 格式化注解
    // 如果不加这个，前端收到的可能是 [2025, 12, 5, 10, 20, 30] 这种数组形式，或者带 T 的 ISO 格式
    // 加了之后，前端收到的就是 "2025-12-05 12:00:00" 字符串
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}