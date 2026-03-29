package com.example.personal_loan.service.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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

    // 定义允许的文件类型白名单
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "pdf", "doc", "docx", "txt"
    ));

    // 定义最大文件大小 (例如：10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; 

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
            log.warn("上传文件为空");
            return null;
        }
        // 校验文件扩展名
        String fileExtension = FileNamingUtil.getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase())) {
            log.warn("文件类型 {} 不被允许", fileExtension);
            return null;
        }
        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("文件大小 {} 超过最大限制 {}", file.getSize(), MAX_FILE_SIZE);
            return null;
        }

        try {
            // 1. 生成安全文件名
            String filename = FileNamingUtil.generateFileName(prefix, userId, file.getOriginalFilename());

            // 2. 获取项目根目录
            // getClass().getResource("/") 获取的是 target/classes 目录
            // .getParent() 获取 target 目录
            // .getParent() 再次获取 personal-loan 模块根目录
            Path moduleRoot = Paths.get(Objects.requireNonNull(this.getClass().getResource("/")).toURI())
                    .getParent().getParent();

            // 3. 构建物理存储路径
            String baseDirConfig = fileStorageConfig.getBaseDir(); // ./uploads
            Path baseDirPath = moduleRoot.resolve(baseDirConfig);

            Path uploadDir = baseDirPath.resolve(subDirPath);
            log.info("准备创建文档目录: {}", uploadDir.toAbsolutePath());
            Files.createDirectories(uploadDir); // 如果不存在，创建多级目录

            // 4. 保存文件
            Path targetPath = uploadDir.resolve(filename);
            file.transferTo(targetPath.toFile());

            // 5. 返回用于数据库存储的 Web 路径
            return subDirPath + "/" + filename;

        } catch (IOException | URISyntaxException e) {
            log.error("文件存储失败: prefix={}, userId={}, error={}", prefix, userId, e.getMessage(), e);
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
        if (relativePath == null || relativePath.isEmpty()) {
            return false;
        }

        try {
            // 1. 获取模块根目录
            Path moduleRoot = Paths.get(Objects.requireNonNull(this.getClass().getResource("/")).toURI())
                    .getParent().getParent();

            // 2. 构建基础路径
            String baseDirConfig = fileStorageConfig.getBaseDir();
            Path baseDirPath = moduleRoot.resolve(baseDirConfig).normalize();

            // 3. 构建目标文件路径并规范化
            Path filePath = baseDirPath.resolve(relativePath).normalize();

            // 4. 安全校验：防止路径遍历攻击
            if (!filePath.startsWith(baseDirPath)) {
                log.warn("非法的文件删除尝试 (路径遍历风险): {}", relativePath);
                return false;
            }

            // 5. 执行删除
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                log.info("文件删除成功: {}", relativePath);
            } else {
                log.debug("文件未找到，无需删除: {}", relativePath);
            }
            return true;

        } catch (IOException | URISyntaxException e) {
            log.error("文件删除失败: {}, 错误: {}", relativePath, e.getMessage(), e);
            return false;
        }
    }
}
