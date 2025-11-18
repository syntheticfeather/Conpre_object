package com.example.personal_loan.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    
    private final int code;
    
    public BusinessException(int code,String message) {
        super(message);
        this.code = code;
    }
    
    public BusinessException(String message) {
        super(message);
        this.code = 400; // 贷款产品增删改查相关错误
    }
}
