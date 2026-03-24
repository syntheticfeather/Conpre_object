package com.example.personal_loan.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.personal_loan.config.FileStorageConfig;
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
     * @return 返回 Web 可访问的相对路径，如 "immovables/property/property_123_20251205_abcd.jpg"
     *         若文件为空，返回 null
     */
    @Override
    public String storeFile(MultipartFile file, String prefix, Long userId, String subDirPath) {
        if (file == null || file.isEmpty()) {
            System.out.println("The File is empty or null");
            return null;
        }

        try {
            // 1. 生成安全文件名
           
            String filename = FileNamingUtil.generateFileName(prefix, userId, file.getOriginalFilename());

            // 2. 构建物理存储路径
            String baseDir = fileStorageConfig.getBaseDir(); // "D:/Conpre_object/.../uploads"
            Path uploadDir = Paths.get(baseDir, subDirPath);   // e.g., D:/.../uploads + "immovables/property"
            log.info("The document to be created: {}", uploadDir.toAbsolutePath());
            Files.createDirectories(uploadDir); // 如果不存在，创建多级目录

            // 3. 保存文件
            Path targetPath = uploadDir.resolve(filename);
            file.transferTo(targetPath.toFile()); 

            // 4. 返回用于数据库存储的 Web 路径
            return subDirPath + "/" + filename;

        } catch (IOException e) {
            log.error("The file stored failed: prefix={}, userId={}, error={}", prefix, userId, e.getMessage(), e);
            throw new RuntimeException("文件存储失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文件
     *
     * @param relativePath 相对路径，例如 "avatars/avatar_1_abc.jpg"
     * @return boolean true 表示删除成功或文件不存在，false 表示删除失败（如权限问题）
     */
    @Override
    public boolean deleteFile(String relativePath) {
        // 1. 校验路径非空
        if (relativePath == null || relativePath.isEmpty()) {
            return false;
        }

        try {
            // 1. 拼接物理路径
            // baseDir: D:/.../uploads
            // relativePath: avatars/avatar_1_abc.jpg
            // 结果: D:/.../uploads/avatars/avatar_1_abc.jpg
            Path filePath = Paths.get(fileStorageConfig.getBaseDir(), relativePath);

            // 2. 安全校验：防止路径遍历攻击
            // 确保解析后的路径仍然在 baseDir 目录下
            Path baseDirPath = Paths.get(fileStorageConfig.getBaseDir()).normalize();
            
            // 如果用户传入 "../../windows/system32/xxx"，normalize 后会跳出 baseDir，校验将失败
            if (!filePath.normalize().startsWith(baseDirPath)) {
                log.warn("illegal file delete attempt (path traversal risk): {}", relativePath);
                return false;
            }

            // 3. 执行删除
            boolean deleted = Files.deleteIfExists(filePath);
            
            if (deleted) {
                log.info("file: {} is deleted successfully", relativePath);
            } else {
                log.debug("file: {} is not found, no need to delete", relativePath);
            }
            return true;

        } catch (IOException e) {
            log.error("file: {} delete failed, error: {}", relativePath, e.getMessage(), e);
            return false;
        }
    }
}
