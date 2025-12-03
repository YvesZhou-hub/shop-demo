package com.shop.demo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;          // 用户ID
    private String username;  // 用户名
    private String password;  // 密码
    private String phone;     // 手机号
    private LocalDateTime createTime;  // 创建时间
}