package com.shop.demo.exception;

/**
 * 库存不足异常
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}

