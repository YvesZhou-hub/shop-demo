package com.shop.demo.exception;

/**
 * 商品不存在异常
 */
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message) {
        super(message);
    }
}
