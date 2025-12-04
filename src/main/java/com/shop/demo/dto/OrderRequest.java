package com.shop.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 请求 DTO：用于下单请求，仅包含客户端可提供的字段
 */
public class OrderRequest {
    @NotNull(message = "用户 ID 不能为空")
    @Min(value = 1, message = "用户 ID 必须大于 0")
    private Long userId;

    @NotNull(message = "商品 ID 不能为空")
    @Min(value = 1, message = "商品 ID 必须大于 0")
    private Long productId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于 0")
    private Integer num;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }
}