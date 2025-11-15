package com.example.personal_loan.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.personal_loan.exception.BusinessException;

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
        
        if(e.getCode().equals("404")){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleAuthError(InvalidCredentialsException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", 401);
        body.put("message", "手机号或密码错误");
        body.put("data", null); 
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // 处理参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException e) {
            log.warn("参数校验失败：{}", e.getMessage());

        // 构建字段错误详情 map
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage,
                (existing, replacement) -> existing // 如果同一字段有多个错误，保留第一个
            ));

        Map<String, Object> response = new HashMap<>();
        response.put("code", 400);
        response.put("message", "请求参数校验失败");
        response.put("data", fieldErrors); // ← 把所有错误放在这里！

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
}
