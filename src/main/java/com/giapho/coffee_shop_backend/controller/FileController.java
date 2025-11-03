package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.FileUploadResponse;
import com.giapho.coffee_shop_backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Uploading file: {}", file.getOriginalFilename());

        String fileName = fileStorageService.storeFile(file);
        String fileUrl = fileStorageService.getFileUrl(fileName);

        FileUploadResponse response = FileUploadResponse.builder()
                .fileName(fileName)
                .fileUrl(fileUrl)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .message("File uploaded successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload-multiple")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<FileUploadResponse[]> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files
    ) {
        log.info("Uploading {} files", files.length);

        FileUploadResponse[] responses = new FileUploadResponse[files.length];

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String fileName = fileStorageService.storeFile(file);
            String fileUrl = fileStorageService.getFileUrl(fileName);

            responses[i] = FileUploadResponse.builder()
                    .fileName(fileName)
                    .fileUrl(fileUrl)
                    .fileSize(file.getSize())
                    .fileType(file.getContentType())
                    .message("File uploaded successfully")
                    .build();
        }

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileName,
            HttpServletRequest request
    ) {
        log.debug("Downloading file: {}", fileName);

        Resource resource = fileStorageService.loadFileAsResource(fileName);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.warn("Could not determine file type for: {}", fileName);
        }
        
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
    
    @DeleteMapping("/{fileName:.+}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<String> deleteFile(@PathVariable String fileName) {
        log.info("Deleting file: {}", fileName);

        fileStorageService.deleteFile(fileName);
        return ResponseEntity.ok("File deleted successfully: " + fileName);
    }
}