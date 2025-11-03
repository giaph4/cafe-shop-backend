package com.giapho.coffee_shop_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "file.storage")
@Data
public class FileStorageProperties {

    private String uploadDir = "uploads/products";

    private long maxFileSize = 5242880; // 5MB

    private String[] allowedExtensions = {"jpg", "jpeg", "png", "gif", "webp"};

    private String baseUrl = "http://localhost:8088";
}