package com.shop.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.shop.demo.mapper") // 确保 Mapper 扫描路径正确
@EnableTransactionManagement // 开启事务管理
public class ShopDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopDemoApplication.class, args);
    }
}
