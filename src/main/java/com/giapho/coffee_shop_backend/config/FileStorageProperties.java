package com.giapho.coffee_shop_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties cho file storage
 */
@Configuration
@ConfigurationProperties(prefix = "file.storage")
@Data
public class FileStorageProperties {

    /**
     * Đường dẫn thư mục lưu trữ file upload
     */
    private String uploadDir = "uploads/products";

    /**
     * Kích thước file tối đa (bytes) - Default: 5MB
     */
    private long maxFileSize = 5242880; // 5MB

    /**
     * Các định dạng file được phép
     */
    private String[] allowedExtensions = {"jpg", "jpeg", "png", "gif", "webp"};

    /**
     * Base URL để truy cập file
     */
    private String baseUrl = "http://localhost:8088";
}