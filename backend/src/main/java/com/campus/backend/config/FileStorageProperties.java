package com.campus.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.upload")
public class FileStorageProperties {
    private String basePath = "./uploads";
    private long imageMaxSize = 10485760; // 10MB
    private long avatarMaxSize = 5242880; // 5MB
    private String allowedTypes = "image/jpeg,image/png,image/webp";
    private String allowedExtensions = "jpg,jpeg,png,webp";
    private double compressionQuality = 0.8;
    private int maxWidth = 1920;
    private int maxHeight = 1080;
    private long chunkSize = 2097152; // 2MB
    private String tempPath = "./uploads/temp";
}
