package org.shvetsov.filestorage.config;

import io.minio.MinioClient;
import org.shvetsov.filestorage.configurations.StorageProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestMinioConfig {

    @Bean
    @Primary
    public MinioClient testMinioClient() {
        // Для тестов используем мок вместо реального клиента
        return mock(MinioClient.class);
    }

    @Bean
    @Primary
    public StorageProperties testStorageProperties() {
        StorageProperties properties = new StorageProperties();
        properties.setBucket("test-bucket");
        properties.setEndpoint("http://localhost:9000");
        properties.setAccessKey("minioadmin");
        properties.setSecretKey("minioadmin");
        properties.setConnectTimeout(30000);
        properties.setWriteTimeout(30000);
        properties.setReadTimeout(30000);
        return properties;
    }
}
