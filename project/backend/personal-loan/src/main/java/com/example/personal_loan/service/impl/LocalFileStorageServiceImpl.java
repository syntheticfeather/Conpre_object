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
import com.example.personal_loan.utils.FileNamingUtil;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LocalFileStorageServiceImpl implements LocalFileStorageService{

    @Autowired
    private FileStorageConfig fileStorageConfig;

    /**
     * 通用认证文件存储方法
     *
     * @param file       上传的文件
     * @param prefix     业务前缀
     * @param userId     用户ID
     * @param subDirPath 相对子目录（从 fileStorageConfig.paths 获取）
     * @return 返回 Web 可访问的相对路径，如 "/uploads/immovables/property/property_123_20251205_abcd.jpg"
     *         若文件为空，返回 null
     */
    @Override
    public String storeFile(MultipartFile file, String prefix, Long userId, String subDirPath) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400,"图片为空");
        }

        try {
            // 1. 生成安全文件名
           
            String filename = FileNamingUtil.generateFileName(prefix, userId, file.getOriginalFilename());

            // 2. 构建物理存储路径
            String baseDir = fileStorageConfig.getBaseDir(); // "D:/Conpre_object/.../uploads"
            Path uploadDir = Paths.get(baseDir, subDirPath);   // e.g., D:/.../uploads + "immovables/property"
            Files.createDirectories(uploadDir); // 如果不存在，创建多级目录

            // 3. 保存文件
            Path targetPath = uploadDir.resolve(filename);
            file.transferTo(targetPath.toFile()); 

            // 4. 返回用于数据库存储的 Web 路径（固定以 /uploads/ 开头）
            return "/uploads/" + subDirPath + "/" + filename;

        } catch (IOException e) {
            throw new RuntimeException("文件存储失败: " + e.getMessage(), e);
        }
    }
}
