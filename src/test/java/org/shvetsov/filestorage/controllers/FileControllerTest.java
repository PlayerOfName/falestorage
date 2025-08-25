package org.shvetsov.filestorage.controllers;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.shvetsov.requestApi.ProductPhotoRS;
import org.shvetsov.responseApi.FileInfoResponse;
import org.shvetsov.responseApi.FileResponse;
import org.shvetsov.storage.StorageException;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.shvetsov.filestorage.services.StorageService;
import org.shvetsov.filestorage.services.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@WebMvcTest(ProductFileController.class)
@Import({org.shvetsov.filestorage.services.FileService.class})
@AutoConfigureMockMvc
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageService storageService;

    @MockBean
    private FileService fileService;

    @Test
    void uploadProductPhoto_Success() throws Exception {
        // Arrange
        UUID productId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);

        when(file.getOriginalFilename()).thenReturn("photo.jpg");
        when(storageService.generateObjectName(productId, fileId, "photo.jpg"))
                .thenReturn(productId + "/" + fileId + ".jpg");
        when(storageService.uploadFile(file, productId + "/" + fileId + ".jpg"))
                .thenReturn(productId + "/" + fileId + ".jpg");
        when(storageService.getFileUrl(productId + "/" + fileId + ".jpg", 7))
                .thenReturn("http://minio/test-url");

        // Act
        ProductPhotoRS response = fileService.uploadProductPhoto(productId, fileId, file);

        // Assert
        assertNotNull(response);
        assertEquals(productId, response.getProductId());
        assertEquals(productId + "/" + fileId + ".jpg", response.getPath());

        verify(storageService).generateObjectName(productId, fileId, "photo.jpg");
        verify(storageService).uploadFile(file, productId + "/" + fileId + ".jpg");
        verify(storageService).getFileUrl(productId + "/" + fileId + ".jpg", 7);
    }

    @Test
    void uploadProductPhoto_WhenStorageException_ThrowsStorageException() throws Exception {
        // Arrange
        UUID productId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);

        when(file.getOriginalFilename()).thenReturn("photo.jpg");
        when(storageService.generateObjectName(productId, fileId, "photo.jpg"))
                .thenReturn(productId + "/" + fileId + ".jpg");
        when(storageService.uploadFile(file, productId + "/" + fileId + ".jpg"))
                .thenThrow(new RuntimeException("Upload failed"));

        // Act & Assert
        assertThrows(StorageException.class,
                () -> fileService.uploadProductPhoto(productId, fileId, file));
    }

    @Test
    void deleteProductPhoto_Success() throws Exception {
        // Arrange
        String path = "products/" + UUID.randomUUID() + "/file.jpg";
        when(storageService.fileExists(path)).thenReturn(true);

        // Act
        assertDoesNotThrow(() -> fileService.deleteProductPhoto(path));

        // Assert
        verify(storageService).fileExists(path);
        verify(storageService).deleteFile(path);
    }

    @Test
    void deleteProductPhoto_WhenFileNotExists_DoesNothing() throws Exception {
        // Arrange
        String path = "products/" + UUID.randomUUID() + "/file.jpg";
        when(storageService.fileExists(path)).thenReturn(false);

        // Act
        assertDoesNotThrow(() -> fileService.deleteProductPhoto(path));

        // Assert
        verify(storageService).fileExists(path);
        verify(storageService, never()).deleteFile(path);
    }

    @Test
    void deleteProductPhoto_WhenStorageException_ThrowsStorageException() throws Exception {
        // Arrange
        String path = "products/" + UUID.randomUUID() + "/file.jpg";
        when(storageService.fileExists(path)).thenReturn(true);
        doThrow(new RuntimeException("Delete failed")).when(storageService).deleteFile(path);

        // Act & Assert
        assertThrows(StorageException.class,
                () -> fileService.deleteProductPhoto(path));
    }

    @Test
    void viewFile_Success() throws Exception {
        // Arrange
        Resource resource = new ByteArrayResource("test".getBytes());
        FileResponse response = FileResponse.builder()
                .resource(resource)
                .fileName("test.txt")
                .contentType("text/plain")
                .size(4L)
                .lastModified(Instant.now())
                .build();

        when(fileService.getFile("test.txt")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/files/view")
                        .param("path", "test.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/plain"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"test.txt\""))
                .andExpect(content().bytes("test".getBytes()));
    }

    @Test
    void downloadFile_Success() throws Exception {
        // Arrange
        Resource resource = new ByteArrayResource("test".getBytes());
        FileResponse response = FileResponse.builder()
                .resource(resource)
                .fileName("test.txt")
                .contentType("text/plain")
                .size(4L)
                .lastModified(Instant.now())
                .build();

        when(fileService.downloadFile("test.txt")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/files/download")
                        .param("path", "test.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"test.txt\""))
                .andExpect(content().bytes("test".getBytes()));
    }

    @Test
    void getFileInfo_Success() throws Exception {
        // Arrange
        FileInfoResponse response = FileInfoResponse.builder()
                .path("test.txt")
                .fileName("test.txt")
                .contentType("text/plain")
                .size(4L)
                .lastModified(Instant.now())
                .url("http://test.url")
                .build();

        when(fileService.getFileInfo("test.txt")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/files/info")
                        .param("path", "test.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("test.txt"))
                .andExpect(jsonPath("$.fileName").value("test.txt"));
    }

    @Test
    void deleteFile_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/files")
                        .param("path", "test.txt"))
                .andExpect(status().isNoContent());

        verify(fileService).deleteProductPhoto("test.txt");
    }
}
