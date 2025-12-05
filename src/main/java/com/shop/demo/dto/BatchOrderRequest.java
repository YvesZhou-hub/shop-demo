package com.shop.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchOrderRequest {

    @NotNull(message = "用户 ID 不能为空")
    @Min(value = 1, message = "用户 ID 必须大于 0")
    private Long userId;

    @NotEmpty(message = "商品列表不能为空")
    @Valid // 触发内部 List 元素的校验
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        @NotNull(message = "商品 ID 不能为空")
        private Long productId;

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量必须大于 0")
        private Integer num;
    }
}