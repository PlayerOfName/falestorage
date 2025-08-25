package org.shvetsov.filestorage.service;

import io.minio.StatObjectResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shvetsov.filestorage.services.FileService;
import org.shvetsov.filestorage.services.StorageService;
import org.shvetsov.requestApi.ProductPhotoRS;
import org.shvetsov.responseApi.FileResponse;
import org.shvetsov.responseApi.FileUploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private StorageService storageService;

    @InjectMocks
    private FileService fileService;

    @Test
    void uploadFile_Success() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        when(file.getSize()).thenReturn(4L);
        when(file.getContentType()).thenReturn("image/jpeg"); // ← ИЗМЕНИТЕ НА image/*
        when(file.getOriginalFilename()).thenReturn("test.jpg");

        // Act
        ProductPhotoRS result = fileService.uploadProductPhoto(productId, fileId, file);

        // Assert
        assertNotNull(result);
    }

    @Test
    void getFile_Success() throws FileNotFoundException {
        // Arrange
        Resource resource = mock(Resource.class);
        StatObjectResponse metadata = mock(StatObjectResponse.class);

        when(storageService.fileExists("test.txt")).thenReturn(true);
        when(storageService.getFileAsResource("test.txt")).thenReturn(resource);
        when(storageService.getFileMetadata("test.txt")).thenReturn(metadata);
        when(metadata.contentType()).thenReturn("text/plain");
        when(metadata.size()).thenReturn(4L);

        // Act
        FileResponse response = fileService.getFile("test.txt");

        // Assert
        assertNotNull(response);
        assertEquals(resource, response.getResource());
    }

    @Test
    void getFile_WhenFileNotExists_ThrowsException() {
        // Arrange
        when(storageService.fileExists("test.txt")).thenReturn(false);

        // Act & Assert
        assertThrows(FileNotFoundException.class, () -> fileService.getFile("test.txt"));
    }

    @Test
    void deleteFile_Success() throws Exception {
        // Arrange
        when(storageService.fileExists("test.txt")).thenReturn(true);

        // Act
        assertDoesNotThrow(() -> fileService.deleteProductPhoto("test.txt"));

        // Assert
        verify(storageService).deleteFile("test.txt");
    }
}
