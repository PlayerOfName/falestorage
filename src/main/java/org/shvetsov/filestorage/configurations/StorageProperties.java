package org.shvetsov.filestorage.configurations;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "minio")
public class StorageProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private int connectTimeout = 30_000; // 30 seconds
    private int writeTimeout = 30_000;   // 30 seconds
    private int readTimeout = 30_000;    // 30 seconds
}
