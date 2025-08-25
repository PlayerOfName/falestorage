package org.shvetsov.filestorage.service;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shvetsov.filestorage.configurations.StorageProperties;
import org.shvetsov.filestorage.services.StorageService;
import org.shvetsov.storage.StorageException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private StorageProperties storageProperties;

    @InjectMocks
    private StorageService storageService;

    @BeforeEach
    void setUp() {
        when(storageProperties.getBucket()).thenReturn("test-bucket");
    }

    @Test
    void uploadFile_Success() throws Exception {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        when(file.getSize()).thenReturn(4L);
        when(file.getContentType()).thenReturn("text/plain");

        // Act
        String result = storageService.uploadFile(file, "test.txt");

        // Assert
        assertEquals("test.txt", result);
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void getFileAsResource_Success() throws Exception {
        // Arrange
        GetObjectResponse getObjectResponse = mock(GetObjectResponse.class);
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenReturn(getObjectResponse);

        // Act
        Resource resource = storageService.getFileAsResource("test.txt");

        // Assert
        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    void fileExists_WhenFileExists_ReturnsTrue() throws Exception {
        // Arrange
        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenReturn(mock(StatObjectResponse.class));

        // Act
        boolean exists = storageService.fileExists("test.txt");

        // Assert
        assertTrue(exists);
    }

    @Test
    void fileExists_WhenFileNotExists_ReturnsFalse() throws Exception {
        // Arrange
        ErrorResponseException errorResponseException = mock(ErrorResponseException.class);
        ErrorResponse errorResponse = mock(ErrorResponse.class);

        when(errorResponse.code()).thenReturn("NoSuchKey");
        when(errorResponseException.errorResponse()).thenReturn(errorResponse);

        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenThrow(errorResponseException);

        // Act
        boolean exists = storageService.fileExists("test.txt");

        // Assert
        assertFalse(exists);
    }

    @Test
    void fileExists_WhenOtherError_ThrowsException() throws Exception {
        // Arrange
        ErrorResponseException errorResponseException = mock(ErrorResponseException.class);
        ErrorResponse errorResponse = mock(ErrorResponse.class);

        when(errorResponse.code()).thenReturn("AccessDenied");
        when(errorResponseException.errorResponse()).thenReturn(errorResponse);

        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenThrow(errorResponseException);

        // Act & Assert
        assertThrows(StorageException.class, () -> storageService.fileExists("test.txt"));
    }

    @Test
    void fileExists_WhenErrorResponseIsNull_ThrowsException() throws Exception {
        // Arrange
        ErrorResponseException errorResponseException = mock(ErrorResponseException.class);
        when(errorResponseException.errorResponse()).thenReturn(null);

        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenThrow(errorResponseException);

        // Act & Assert
        assertThrows(StorageException.class, () -> storageService.fileExists("test.txt"));
    }

    @Test
    void fileExists_WhenGenericException_ThrowsStorageException() throws Exception {
        // Arrange
        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenThrow(new RuntimeException("Generic error"));

        // Act & Assert
        assertThrows(StorageException.class, () -> storageService.fileExists("test.txt"));
    }
}