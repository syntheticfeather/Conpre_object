package com.example.personal_loan.handler;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.personal_loan.dto.ApiResult;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.exception.InvalidCredentialsException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<?>> handleBusinessException(BusinessException e) {
        log.warn("业务异常:code={}, message={}", e.getCode(), e.getMessage());

        ApiResult<?> response = ApiResult.fail(e.getCode(), e.getMessage());

        if (e.getCode() == 404) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResult<?>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage(), e);

        ApiResult<?> response = ApiResult.fail(500, "系统内部错误: " + e.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<?>> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);

        ApiResult<?> response = ApiResult.fail(500, "系统内部错误: " + e.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResult<?>> handleAuthError(InvalidCredentialsException e) {
        ApiResult<?> response = ApiResult.fail(401, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // 处理参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<?>> handleValidationExceptions(MethodArgumentNotValidException e) {
        log.warn("参数校验失败：{}", e.getMessage());

        // 构建字段错误详情 map
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing // 如果同一字段有多个错误，保留第一个
                ));

        ApiResult<?> response = ApiResult.fail(400, fieldErrors, "请求参数校验失败");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

}
