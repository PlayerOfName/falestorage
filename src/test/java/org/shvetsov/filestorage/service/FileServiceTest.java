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

import java.io.FileNotFoundException;
import java.time.Instant;
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
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.txt");
        when(file.getSize()).thenReturn(4L);
        when(file.getContentType()).thenReturn("text/plain");

        when(storageService.uploadFile(any(), any())).thenReturn("test.txt");
        when(storageService.getFileUrl(any(), anyInt())).thenReturn("http://test.url");

        UUID productId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        // Act
        ProductPhotoRS response = fileService.uploadProductPhoto(productId, fileId, file);

        // Assert
        assertNotNull(response);
        assertEquals("test.txt", response.getPath());
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
        when(metadata.lastModified()).thenReturn(ZonedDateTime.from(Instant.now()));

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
