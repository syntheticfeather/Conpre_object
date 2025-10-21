package com.example.personal_loan.exception;

public enum ErrorCode {
    USER_NOT_FOUND("USER_001", "用户不存在"),
    PHONE_EXISTS("USER_002", "手机号已被注册"),
    ID_CARD_EXISTS("USER_003", "身份证号已被使用"),
    PASSWORD_ERROR("AUTH_001", "密码错误"),
    INVALID_PHONE("VALID_001", "手机号格式不正确"),
    INVALID_PASSWORD("VALID_002", "密码不符合要求");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() { 
        return code; 
    }
    public String getMessage() { 
        return message; 
    }
}