package com.example.personal_loan.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.personal_loan.config.FileStorageConfig;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.service.LocalFileStorageService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LocalFileStorageServiceImpl implements LocalFileStorageService{

    @Autowired
    private FileStorageConfig fileStorageConfig;

    // 将相对路径转为绝对路径
    @Override
    public Path resolvePath(String relativeSubPath) {
        return Paths.get(fileStorageConfig.getBaseDir(), relativeSubPath);
    }

    @Override
    public void createDirectoriesIfNotExist(Path dir) {
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                log.error("创建目录失败: {}", dir, e);
                throw new BusinessException(500, "文件存储服务异常");
            }
        }
    }

    @Override
    public void storeFile(Path filePath, MultipartFile file) throws IOException {
        file.transferTo(filePath.toFile());
    }
}
