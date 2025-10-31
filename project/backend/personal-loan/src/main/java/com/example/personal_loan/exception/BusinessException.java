package com.example.personal_loan.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    
    private final String code;
    
    public BusinessException(String code,String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
    
    public BusinessException(String message) {
        super(message);
        this.code = "LOAN_PRODUCT_ERROR"; // 贷款产品增删改查相关错误
    }
}
