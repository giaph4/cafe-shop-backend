package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.FileUploadResponse;
import com.giapho.coffee_shop_backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Arrays;

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
        return ResponseEntity.ok(buildUploadResponse(file));
    }

    @PostMapping("/upload-multiple")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<FileUploadResponse[]> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files
    ) {
        log.info("Uploading {} files", files.length);

        FileUploadResponse[] responses = Arrays.stream(files)
                .map(this::buildUploadResponse)
                .toArray(FileUploadResponse[]::new);

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileName,
            HttpServletRequest request
    ) {
        log.debug("Downloading file: {}", fileName);

        Resource resource = fileStorageService.loadFileAsResource(fileName);
        String contentType = determineContentType(resource, request);

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

    private FileUploadResponse buildUploadResponse(MultipartFile file) {
        String storedName = fileStorageService.storeFile(file);
        return FileUploadResponse.builder()
                .fileName(storedName)
                .fileUrl(fileStorageService.getFileUrl(storedName))
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .message("File uploaded successfully")
                .build();
    }

    private String determineContentType(Resource resource, HttpServletRequest request) {
        return MediaTypeFactory.getMediaType(resource)
                .map(MediaType::toString)
                .orElseGet(() -> resolveFromServletContext(resource, request));
    }

    private String resolveFromServletContext(Resource resource, HttpServletRequest request) {
        try {
            String servletType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            return servletType != null ? servletType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        } catch (IOException ex) {
            log.debug("Fallback to default content type for {}", resource.getFilename(), ex);
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}