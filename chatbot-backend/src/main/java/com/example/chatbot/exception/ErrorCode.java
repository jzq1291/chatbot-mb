package com.example.chatbot.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 用户相关错误 (1000-1999)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "用户不存在"),
    USERNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "USER_002", "用户名已存在"),
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "USER_003", "邮箱已被使用"),
    
    // 认证相关错误 (2000-2999)
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_001", "用户名或密码错误"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_002", "没有权限访问该资源"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_003", "登录已过期，请重新登录"),
    
    // 参数验证错误 (3000-3999)
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "VALID_001", "输入参数有误"),
    
    // 系统错误 (5000-5999)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS_001", "服务器内部错误");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
} 