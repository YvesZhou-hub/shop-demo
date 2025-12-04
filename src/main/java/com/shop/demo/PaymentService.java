package com.shop.demo;


import com.shop.demo.entity.Payment;

import java.util.Map;

public interface PaymentService {
    Payment createPayment(Payment payment); // 创建记录并返回（包含 paymentNo）
    Payment getByPaymentNo(String paymentNo);
    boolean markPaid(String paymentNo, String providerTradeNo); // 幂等地标记为已支付

    boolean verifySignature(String provider, Map<String, String> params);
}