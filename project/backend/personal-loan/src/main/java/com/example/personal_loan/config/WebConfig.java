package com.example.personal_loan.config;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

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
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/refresh-token",
                        "/static/**",
                        "/JS/**",
                        "/templates/**");
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

        String location = resolveUploadsLocation();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

    private String resolveUploadsLocation() {
        String baseDir = fileStorageConfig.getBaseDir();
        if (baseDir == null || baseDir.isBlank()) {
            return "file:./uploads/";
        }

        if (baseDir.startsWith("file:")) {
            return baseDir.endsWith("/") ? baseDir : baseDir + "/";
        }

        Path basePath = Paths.get(baseDir);
        if (!basePath.isAbsolute()) {
            try {
                Path moduleRoot = Paths.get(Objects.requireNonNull(WebConfig.class.getResource("/")).toURI())
                        .getParent()
                        .getParent();
                basePath = moduleRoot.resolve(baseDir);
            } catch (URISyntaxException | NullPointerException e) {
                basePath = Paths.get(System.getProperty("user.dir")).resolve(baseDir);
            }
        }

        String location = basePath.normalize().toAbsolutePath().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }

}
