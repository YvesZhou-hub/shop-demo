package com.shop.demo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
public class Order implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // 使用 ToStringSerializer 防止前端 Long 类型精度丢失
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    @NotNull(message = "商品 ID 不能为空")
    private Long productId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于 0")
    private Integer num;

    private BigDecimal totalPrice;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}