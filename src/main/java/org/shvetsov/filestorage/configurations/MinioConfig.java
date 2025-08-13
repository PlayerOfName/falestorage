package org.shvetsov.filestorage.configurations;

import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MinioConfig {
    private final StorageProperties storageProperties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(storageProperties.getEndpoint())
                .credentials(
                        storageProperties.getAccessKey(),
                        storageProperties.getSecretKey())
                .build();
    }

    @PostConstruct
    public void logProperties() {
        System.out.println("Endpoint = " + storageProperties.getEndpoint());
        System.out.println("AccessKey = " + storageProperties.getAccessKey());
        System.out.println("SecretKey = " + storageProperties.getSecretKey());
        System.out.println("Bucket = " + storageProperties.getBucket());
    }

}
