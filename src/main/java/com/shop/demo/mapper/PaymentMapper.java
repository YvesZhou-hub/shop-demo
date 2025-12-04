package com.shop.demo.mapper;

import com.shop.demo.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentMapper {
    int insertPayment(Payment payment);
    Payment selectByPaymentNo(@Param("paymentNo") String paymentNo);
    int updateStatusByPaymentNo(@Param("paymentNo") String paymentNo, @Param("status") String status, @Param("providerTradeNo") String providerTradeNo);
}