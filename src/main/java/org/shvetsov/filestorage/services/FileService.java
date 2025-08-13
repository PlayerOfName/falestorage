package org.shvetsov.filestorage.services;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.utils.FileNameUtils;
import org.shvetsov.filestorage.configurations.StorageProperties;
import org.shvetsov.requestApi.ProductPhotoRQ;
import org.shvetsov.requestApi.UploadFileRQ;
import org.shvetsov.storage.StorageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService {
    private final StorageService storageService;
    private final MinioClient minioClient;
    private final StorageProperties properties;

    public ProductPhotoRQ uploadProductPhoto(UUID productId, UUID userId, MultipartFile file) {
        // 2. Генерация уникального имени файла
        String objectName = storageService.generateObjectName(productId, userId, UUID.randomUUID(), file.getOriginalFilename());

        try {
            // 3. Загрузка в хранилище
            storageService.uploadFile(file, objectName);

            // 4. Получение URL
            String fileUrl = storageService.getFileUrl(objectName, 7); // Ссылка на 7 дней

            return new ProductPhotoRQ();
        } catch (Exception e) {
            throw new StorageException("Failed to upload file");
        }
    }


    public void deleteProductPhoto(UUID productId, String filename) {
        String objectName = "products/" + productId + "/" + filename;
        try {
            if (storageService.fileExists(objectName)) {
                storageService.deleteFile(objectName);
            }
        } catch (Exception e) {
            throw new StorageException("Failed to delete file");
        }
    }

    public ResponseEntity<byte[]> downloadProductPhoto(UUID productId, String filename) {
        String objectName = "products/" + productId + "/" + filename;
        try {
            byte[] fileBytes = storageService.getFileBytes(objectName);
            StatObjectResponse metadata = storageService.getFileMetadata(objectName);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, metadata.contentType())
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .body(fileBytes);
        } catch (Exception e) {
            throw new StorageException("File download failed");
        }
    }
    public List<ProductPhotoRQ> listFiles(String prefix) throws Exception {
        List<ProductPhotoRQ> photos = new ArrayList<>();

        // Получаем список объектов
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(properties.getBucket())
                        .prefix(prefix)
                        .recursive(false)
                        .build());

        // Для каждого объекта получаем метаданные и создаем ProductPhotoRQ
        for (Result<Item> result : results) {
            Item item = result.get();

            // Получаем полную информацию об объекте
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(item.objectName())
                            .build());

            // Разбираем путь к файлу для получения productId
            // (предполагаем формат: "products/{productId}/filename.jpg")
            String[] pathParts = item.objectName().split("/");
            UUID productId = pathParts.length > 1 ? UUID.fromString(pathParts[1]) : null;

            // Создаем DTO
            ProductPhotoRQ photo = ProductPhotoRQ.builder()
                    .productId(productId)
                    .displayOrder(0)
                    .isMain(false)
                    .build();


            photos.add(photo);
        }

        return photos;
    }
}
