package com.example.personal_loan.service;

import org.springframework.web.multipart.MultipartFile;

public interface LocalFileStorageService {
    String storeFile(MultipartFile file, String prefix, Long userId, String subDirPath);
    boolean deleteFile(String relativePath);
}
