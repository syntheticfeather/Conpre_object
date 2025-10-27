package com.example.personal_loan.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.personal_loan.handler.JwtInterceptor;

public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**") // 拦截所有 /api/ 下的请求
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register"
                ); // 排除登录和注册接口
    }
}
