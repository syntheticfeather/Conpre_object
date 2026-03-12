package com.example.personal_loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "API响应结果")
public class ApiResult<T> {
    @Schema(description = "响应码", example = "200")
    private int code;
    @Schema(description = "响应数据")
    private T data;
    @Schema(description = "响应消息", example = "操作成功")
    private String message;

    // 构造函数
    public ApiResult(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    // Getters and Setters
    public int getCode() { 
        return code; 
    }
    public void setCode(int code) { 
        this.code = code; 
    }

    public T getData() { 
        return data; 
    }
    public void setData(T data) { 
        this.data = data; 
    }

    public String getMessage() { 
        return message; 
    }
    public void setMessage(String message) { 
        this.message = message; 
    }

    // 静态工厂方法（方便使用）
    public static <T> ApiResult<T> success(T data, String message) {
        return new ApiResult<>(200, data, message);
    }

    public static <T> ApiResult<T> success(T data) {
        return success(data, "操作成功");
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        return new ApiResult<>(code, null, message);
    }

    public static <T> ApiResult<T> fail(int code, T data, String message) {
        return new ApiResult<>(code, data, message);
    }
}