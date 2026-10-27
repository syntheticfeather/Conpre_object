package com.example.personal_loan.utils;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FileNamingUtil {
    private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int RANDOM_LENGTH = 6;

    /**
     * 生成标准化文件名
     * 格式: {prefix}_{id}_{yyyyMMdd}_{random}.{ext}
     *
     * 扩展名合法性由调用方（如 LocalFileStorageServiceImpl 的白名单）校验，
     * 这里只负责提取和规范化，避免两处白名单不一致导致 PDF 等文件被误拒。
     *
     * @param prefix     业务前缀，如 "avatar"
     * @param id         用户ID、申请ID等业务ID
     * @param originalName 原始文件名（用于提取扩展名）
     * @return 标准化文件名，如 avatar_123_20251202_a3x9qz.jpg
     */
    public static String generateFileName(String prefix, Long id, String originalName) {
        // 只提取扩展名，是否允许由调用方按业务白名单判断
        String ext = "";
        if (originalName != null && originalName.lastIndexOf('.') > 0) {
            ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
        }

        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD
        String randomStr = generateRandomString(RANDOM_LENGTH);

        return String.format("%s_%d_%s_%s%s", prefix, id, dateStr, randomStr, ext);
    }

    private static String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM_CHARS.charAt(random.nextInt(RANDOM_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 获取文件扩展名
     * @param fileName 文件名（例如：avatar.png 或 report.final.pdf）
     * @return 扩展名（例如：png 或 pdf），如果没有扩展名返回空字符串
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        // 获取最后一个点的位置
        int lastIndexOfDot = fileName.lastIndexOf(".");

        // 如果没有点，或者点在文件名开头（如 .htaccess），则无扩展名
        if (lastIndexOfDot == -1 || lastIndexOfDot == 0) {
            return "";
        }

        // 获取点之后的字符串，并转为小写以便统一比较
        return fileName.substring(lastIndexOfDot + 1).toLowerCase();
    }
}
