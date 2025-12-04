package com.example.personal_loan.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.personal_loan.handler.JwtInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private FileStorageConfig fileStorageConfig;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/login/**",
                        "/api/registration/**",
                        "/static/**",
                        "/JS/**",
                        "/templates/**"
                );
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // http:localhost:8080/login
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/registration").setViewName("registration");
        registry.addViewController("/").setViewName("login");
        registry.addViewController("/index").setViewName("index");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/JS/**").addResourceLocations("classpath:/JS/");

        // 映射 /uploads/** 到物理目录
        String location = "file:" + fileStorageConfig.getBaseDir() + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

}
