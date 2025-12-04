package com.shop.demo.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Payment {
    private Long id;
    private String paymentNo;        // 系统支付单号
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private String status;          // PENDING, PAID, FAILED
    private String provider;        // ALIPAY, WECHAT
    private String providerTradeNo; // 支付网关返回的交易号
    private String returnUrl;
    private String notifyUrl;
    private String extra;           // JSON 字符串用于保存订单列表等
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}