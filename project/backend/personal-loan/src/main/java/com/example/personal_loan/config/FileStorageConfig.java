package com.example.personal_loan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;


@ConfigurationProperties(prefix = "file-storage")
@Component
@Data
public class FileStorageConfig {
    
    private String baseDir;
    private Paths paths = new Paths();

    @Data
    public static class Paths {
        private String avatar;
        private String contract;

        // 不动产
        private String carProof;
        private String propertyProof;

        // 第三方认证
        private String creditReport;
        private String socialSecurity;

        // 工作证明
        private String employmentProof;
        private String salaryProof;
    }
}