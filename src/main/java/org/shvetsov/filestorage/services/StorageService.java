package org.shvetsov.filestorage.services;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shvetsov.filestorage.configurations.StorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.minio.http.Method;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {
    private final MinioClient minioClient;
    private final StorageProperties properties;

    // 1. Загрузка файла
    public String uploadFile(MultipartFile file, String objectName) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
        return objectName;
    }

    // 2. Удаление файла
    public void deleteFile(String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectName)
                        .build());
    }

    // 3. Получение файла как byte[]
    public byte[] getFileBytes(String objectName) throws Exception {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectName)
                        .build());
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            stream.transferTo(outputStream);
            return outputStream.toByteArray();
        }
    }

    // 4. Получение временной ссылки на файл
    public String getFileUrl(String objectName, int expiryDays) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(properties.getBucket())
                        .object(objectName)
                        .expiry(expiryDays, TimeUnit.DAYS)
                        .build());
    }

    // 5. Получение списка файлов в папке
    public List<String> listFiles(String prefix) throws Exception {
        List<String> fileNames = new ArrayList<>();
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(properties.getBucket())
                        .prefix(prefix)
                        .recursive(false)
                        .build());

        for (Result<Item> result : results) {
            fileNames.add(result.get().objectName());
        }
        return fileNames;
    }

    // 6. Проверка существования файла
    public boolean fileExists(String objectName) throws Exception {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectName)
                            .build());
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            throw e;
        }
    }

    // 7. Получение метаданных файла
    public StatObjectResponse getFileMetadata(String objectName) throws Exception {
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectName)
                        .build());
    }

    public String generateObjectName(UUID productId, UUID userId, UUID fileId, String originalFilename) {
        // Получаем расширение файла
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

        // Формируем путь в формате: {productId}/{userId}/{fileId}{extension}
        return String.format("%s/%s/%s%s",
                productId.toString(),
                userId.toString(),
                fileId.toString(),
                extension);
    }
}
