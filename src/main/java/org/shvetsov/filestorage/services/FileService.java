package org.shvetsov.filestorage.services;

import io.minio.*;
import io.minio.messages.Item;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.FileNameUtils;
import org.shvetsov.filestorage.configurations.StorageProperties;
import org.shvetsov.requestApi.ProductPhotoRQ;
import org.shvetsov.requestApi.ProductPhotoRS;
import org.shvetsov.requestApi.UploadFileRQ;
import org.shvetsov.responseApi.FileInfoResponse;
import org.shvetsov.responseApi.FileResponse;
import org.shvetsov.storage.StorageException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileService {
    private final StorageService storageService;
    private final MinioClient minioClient;
    private final StorageProperties properties;

    public ProductPhotoRS uploadProductPhoto(UUID productId, UUID fileId, MultipartFile file) {
        if (!file.getContentType().startsWith("image/")) throw new ValidationException("Only images allowed");
        // 2. Генерация уникального имени файла
        String objectName = storageService.generateObjectName(productId, fileId, file.getOriginalFilename());

        try {
            // 3. Загрузка в хранилище
            storageService.uploadFile(file, objectName);

            // 4. Получение URL
            String fileUrl = storageService.getFileUrl(objectName, 7); // Ссылка на 7 дней

            return ProductPhotoRS.builder()
                    .path(objectName)
                    .productId(productId)
                    .build();
        } catch (Exception e) {
            throw new StorageException("Failed to upload file");
        }
    }

    public void deleteProductPhoto(String path) {
        try {
            if (storageService.fileExists(path)) {
                storageService.deleteFile(path);
            }
        } catch (Exception e) {
            throw new StorageException("Failed to delete file");
        }
    }

    // 3. Получение файла для просмотра
    public FileResponse getFile(String path) throws FileNotFoundException {
        try {
            if (!storageService.fileExists(path)) {
                throw new FileNotFoundException("File not found: " + path);
            }

            Resource resource = storageService.getFileAsResource(path);
            StatObjectResponse metadata = storageService.getFileMetadata(path);

            return FileResponse.builder()
                    .resource(resource)
                    .fileName(getFileNameFromPath(path))
                    .contentType(metadata.contentType())
                    .size(metadata.size())
                    .lastModified(metadata.lastModified().toInstant())
                    .build();
        } catch (FileNotFoundException e) {
            log.warn("File not found: {}", path);
            throw e;
        } catch (Exception e) {
            log.error("Failed to get file: {}", path, e);
            throw new StorageException("Failed to get file");
        }
    }

    // 4. Скачивание файла
    public FileResponse downloadFile(String path) throws FileNotFoundException {
        try {
            if (!storageService.fileExists(path)) {
                throw new FileNotFoundException("File not found: " + path);
            }

            Resource resource = storageService.getFileAsResource(path);
            StatObjectResponse metadata = storageService.getFileMetadata(path);

            return FileResponse.builder()
                    .resource(resource)
                    .fileName(getFileNameFromPath(path))
                    .contentType(metadata.contentType())
                    .size(metadata.size())
                    .lastModified(metadata.lastModified().toInstant())
                    .build();
        } catch (FileNotFoundException e) {
            log.warn("File not found for download: {}", path);
            throw e;
        } catch (Exception e) {
            log.error("Failed to download file: {}", path, e);
            throw new StorageException("Failed to download file");
        }
    }

    // 5. Получение информации о файле
    public FileInfoResponse getFileInfo(String path) throws FileNotFoundException {
        try {
            if (!storageService.fileExists(path)) {
                throw new FileNotFoundException("File not found: " + path);
            }

            StatObjectResponse metadata = storageService.getFileMetadata(path);

            return FileInfoResponse.builder()
                    .path(path)
                    .fileName(getFileNameFromPath(path))
                    .contentType(metadata.contentType())
                    .size(metadata.size())
                    .lastModified(metadata.lastModified().toInstant())
                    .url(storageService.getFileUrl(path, 1)) // кратковременная ссылка
                    .build();
        } catch (FileNotFoundException e) {
            log.warn("File not found for info: {}", path);
            throw e;
        } catch (Exception e) {
            log.error("Failed to get file info: {}", path, e);
            throw new StorageException("Failed to get file info");
        }
    }

    // 6. Получение списка файлов в директории
    public List<FileInfoResponse> listFiles(String prefix) {
        try {
            List<FileInfoResponse> files = new ArrayList<>();
            List<String> filePaths = storageService.listFiles(prefix);

            for (String path : filePaths) {
                try {
                    StatObjectResponse metadata = storageService.getFileMetadata(path);
                    files.add(FileInfoResponse.builder()
                            .path(path)
                            .fileName(getFileNameFromPath(path))
                            .contentType(metadata.contentType())
                            .size(metadata.size())
                            .lastModified(metadata.lastModified().toInstant())
                            .build());
                } catch (Exception e) {
                    log.warn("Failed to get metadata for file: {}", path, e);
                }
            }

            return files;
        } catch (Exception e) {
            log.error("Failed to list files with prefix: {}", prefix, e);
            throw new StorageException("Failed to list files");
        }
    }

    // 7. Генерация URL для файла
    public String generateFileUrl(String path, int expiryDays) throws FileNotFoundException {
        try {
            if (!storageService.fileExists(path)) {
                throw new FileNotFoundException("File not found: " + path);
            }
            return storageService.getFileUrl(path, expiryDays);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate URL for file: {}", path, e);
            throw new StorageException("Failed to generate file URL");
        }
    }

    // 8. Проверка существования файла
    public boolean fileExists(String path) {
        return storageService.fileExists(path);
    }

    private String getFileNameFromPath(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }
}
