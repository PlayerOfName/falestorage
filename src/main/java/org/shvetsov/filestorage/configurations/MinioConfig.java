package org.shvetsov.filestorage.configurations;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MinioConfig {
    private final StorageProperties storageProperties;

    @Bean
    public MinioClient minioClient() {
        try {
            // Создаем HTTP клиент с таймаутами
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .connectTimeout(storageProperties.getConnectTimeout(), TimeUnit.MILLISECONDS)
                    .writeTimeout(storageProperties.getWriteTimeout(), TimeUnit.MILLISECONDS)
                    .readTimeout(storageProperties.getReadTimeout(), TimeUnit.MILLISECONDS)
                    .build();

            // Создаем MinIO клиент
            MinioClient client = MinioClient.builder()
                    .endpoint(storageProperties.getEndpoint())
                    .credentials(
                            storageProperties.getAccessKey(),
                            storageProperties.getSecretKey())
                    .httpClient(httpClient)
                    .build();

            // Проверяем и создаем бакет если нужно
            initializeBucket(client);

            return client;
        } catch (Exception e) {
            log.error("Failed to initialize MinIO client", e);
            throw new RuntimeException("MinIO initialization failed", e);
        }
    }

    private void initializeBucket(MinioClient client) {
        try {
            boolean bucketExists = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(storageProperties.getBucket())
                    .build());

            if (!bucketExists) {
                log.warn("Bucket {} does not exist. Creating...", storageProperties.getBucket());
                client.makeBucket(MakeBucketArgs.builder()
                        .bucket(storageProperties.getBucket())
                        .build());
                log.info("Bucket {} created successfully", storageProperties.getBucket());
            } else {
                log.info("Bucket {} already exists", storageProperties.getBucket());
            }
        } catch (Exception e) {
            log.error("Failed to initialize bucket: {}", storageProperties.getBucket(), e);
            throw new RuntimeException("Bucket initialization failed", e);
        }
    }
}
