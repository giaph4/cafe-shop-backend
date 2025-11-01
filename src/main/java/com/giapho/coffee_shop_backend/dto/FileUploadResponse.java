package com.giapho.coffee_shop_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho file upload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    private String fileName;
    private String fileUrl;
    private long fileSize;
    private String fileType;
    private String message;
}