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

    private String[] allowedExtensions = {
            "jpg", "jpeg", "png", "gif", "webp",
            "mp4", "mov", "avi", "mkv",
            "mp3", "wav", "m4a", "aac",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt"
    };

    private String[] imageExtensions = {"jpg", "jpeg", "png", "gif", "webp"};

    private String baseUrl = "http://localhost:8088";
}