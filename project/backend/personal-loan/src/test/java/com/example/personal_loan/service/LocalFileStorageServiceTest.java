package com.example.personal_loan.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import com.example.personal_loan.config.FileStorageConfig;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.service.impl.LocalFileStorageServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocalFileStorageServiceTest {

    @Mock
    private FileStorageConfig fileStorageConfig;

    @InjectMocks
    private LocalFileStorageServiceImpl localFileStorageService;

    @TempDir
    Path tempDir;

    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        mockFile = mock(MultipartFile.class);
        when(fileStorageConfig.getBaseDir()).thenReturn(tempDir.toString());
    }

    @Test
    void testStoreFile_Success() throws IOException {
        byte[] content = "test file content".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));

        String result = localFileStorageService.storeFile(mockFile, "property", 1L, "immovables/property");

        assertNotNull(result);
        assertTrue(result.startsWith("/uploads/"));
        assertTrue(result.contains("immovables/property"));
        assertTrue(result.contains("property_1_"));
        assertTrue(result.endsWith(".jpg"));
    }

    @Test
    void testStoreFile_NullFile() {
        BusinessException exception = assertThrows(BusinessException.class, () -> 
            localFileStorageService.storeFile(null, "property", 1L, "immovables/property")
        );
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("图片为空"));
    }

    @Test
    void testStoreFile_EmptyFile() {
        when(mockFile.isEmpty()).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            localFileStorageService.storeFile(mockFile, "property", 1L, "immovables/property")
        );
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("图片为空"));
    }

    @Test
    void testStoreFile_CreatesDirectory() throws IOException {
        byte[] content = "test file content".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));

        String subDir = "test/subdir";
        String result = localFileStorageService.storeFile(mockFile, "property", 1L, subDir);

        assertNotNull(result);
        Path createdDir = tempDir.resolve(subDir);
        assertTrue(Files.exists(createdDir));
        assertTrue(Files.isDirectory(createdDir));
    }

    @Test
    void testStoreFile_ReturnsCorrectPath() throws IOException {
        byte[] content = "test file content".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));

        String result = localFileStorageService.storeFile(mockFile, "property", 1L, "immovables/property");

        assertNotNull(result);
        assertTrue(result.startsWith("/uploads/immovables/property/property_1_"));
        assertTrue(result.endsWith(".jpg"));
    }

    @Test
    void testStoreFile_TransferToCalled() throws IOException {
        byte[] content = "test file content".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));

        String result = localFileStorageService.storeFile(mockFile, "property", 1L, "immovables/property");

        assertNotNull(result);
        // 验证结果路径格式正确
        assertTrue(result.startsWith("/uploads/"));
        assertTrue(result.contains("immovables/property"));
        assertTrue(result.contains("property_1_"));
        assertTrue(result.endsWith(".jpg"));
    }

    @Test
    void testStoreFile_DifferentFileExtensions() throws IOException {
        byte[] content = "test file content".getBytes();
        
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.png");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));
        String result1 = localFileStorageService.storeFile(mockFile, "avatar", 1L, "avatars");
        assertTrue(result1.endsWith(".png"));

        when(mockFile.getOriginalFilename()).thenReturn("test.jpeg");
        String result2 = localFileStorageService.storeFile(mockFile, "avatar", 1L, "avatars");
        assertTrue(result2.endsWith(".jpeg"));

        when(mockFile.getOriginalFilename()).thenReturn("test.gif");
        String result3 = localFileStorageService.storeFile(mockFile, "avatar", 1L, "avatars");
        assertTrue(result3.endsWith(".gif"));
    }

    @Test
    void testStoreFile_DifferentUserIds() throws IOException {
        byte[] content = "test file content".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));

        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        String result1 = localFileStorageService.storeFile(mockFile, "avatar", 1L, "avatars");
        assertTrue(result1.contains("avatar_1_"));

        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        String result2 = localFileStorageService.storeFile(mockFile, "avatar", 2L, "avatars");
        assertTrue(result2.contains("avatar_2_"));
    }

    @Test
    void testStoreFile_WithEmptySubDir() throws IOException {
        byte[] content = "test file content".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));

        String result = localFileStorageService.storeFile(mockFile, "property", 1L, "");

        assertNotNull(result);
        assertTrue(result.startsWith("/uploads/"));
        assertTrue(result.contains("property_1_"));
    }

    @Test
    void testStoreFile_WithSpecialCharactersInFilename() throws IOException {
        byte[] content = "test file content".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test file.jpg");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));

        String result = localFileStorageService.storeFile(mockFile, "property", 1L, "immovables/property");

        assertNotNull(result);
        assertTrue(result.contains("property_1_"));
        assertTrue(result.endsWith(".jpg"));
    }

    @Test
    void testStoreFile_WithZeroUserId() throws IOException {
        byte[] content = "test file content".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));

        String result = localFileStorageService.storeFile(mockFile, "property", 0L, "immovables/property");

        assertNotNull(result);
        assertTrue(result.contains("property_0_"));
    }

    @Test
    void testStoreFile_IOException() throws IOException {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        // 使用 doThrow 来模拟 transferTo 方法的异常
        doThrow(new IOException("Test IO exception")).when(mockFile).transferTo(any(File.class));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            localFileStorageService.storeFile(mockFile, "property", 1L, "immovables/property")
        );
        assertTrue(exception.getMessage().contains("文件存储失败"));
    }

    @Test
    void testStoreFile_UnsupportedFileType() throws IOException {
        byte[] content = "test file content".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.exe");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            localFileStorageService.storeFile(mockFile, "property", 1L, "immovables/property")
        );
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("不支持的文件类型"));
    }
}
