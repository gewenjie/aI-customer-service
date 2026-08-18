package com.customerservice.dto;

import lombok.Data;

/**
 * 当前登录用户上下文
 */
@Data
public class LoginUser {
    private Long userId;
    private String username;
    private String role;
}
