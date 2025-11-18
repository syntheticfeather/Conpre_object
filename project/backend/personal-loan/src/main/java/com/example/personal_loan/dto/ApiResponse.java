package com.example.personal_loan.dto;

public class ApiResponse<T> {
    private int code;
    private T data;
    private String message;

    // 构造函数
    public ApiResponse(int code, T data, String message) {
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
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(200, data, message);
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "操作成功");
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, null, message);
    }

    public static <T> ApiResponse<T> fail(int code, T data, String message) {
        return new ApiResponse<>(code, data, message);
    }
}