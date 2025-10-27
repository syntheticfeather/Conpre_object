package com.example.personal_loan.exception;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException e) {
        log.warn("业务异常:code={}, message={}", e.getCode(), e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("code", e.getCode());
        response.put("message", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 处理参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException e) {
        log.warn("参数校验失败：{}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("code", "VALID_000");
        response.put("message", "请求参数校验失败");
        // response.put("details", e.getBindingResult().getFieldErrors().stream()
        //         .collect(java.util.stream.Collectors.toMap(
        //                 error -> error.getField(),
        //                 error -> error.getDefaultMessage(),
        //                 (oldValue, newValue) -> oldValue 
        //         )));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
}
