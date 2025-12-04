package com.example.personal_loan.service;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

public interface LocalFileStorageService {
    Path resolvePath(String relativeSubPath);

    void createDirectoriesIfNotExist(Path dir);

    void storeFile(Path filePath, MultipartFile file) throws IOException;
}
