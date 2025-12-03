package com.shop.demo.entity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Order {
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

    private LocalDateTime createTime;
}