package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.config.FileStorageProperties;
import com.giapho.coffee_shop_backend.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.UUID;

/**
 * Service xử lý upload, lưu trữ và truy xuất file
 */
@Service
@Slf4j
public class FileStorageService {

    private final Path fileStorageLocation;
    private final FileStorageProperties fileStorageProperties;

    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("File storage directory created at: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    /**
     * Lưu file và trả về tên file đã lưu
     */
    public String storeFile(MultipartFile file) {
        // Validate file
        validateFile(file);

        // Tạo tên file unique
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String newFileName = generateUniqueFileName() + "." + fileExtension;

        try {
            // Kiểm tra file name không có ký tự đặc biệt
            if (originalFileName.contains("..")) {
                throw new FileStorageException("Filename contains invalid path sequence: " + originalFileName);
            }

            // Copy file vào thư mục đích
            Path targetLocation = this.fileStorageLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored successfully: {} (original: {})", newFileName, originalFileName);
            return newFileName;

        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + newFileName + ". Please try again!", ex);
        }
    }

    /**
     * Load file như một Resource
     */
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new FileStorageException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new FileStorageException("File not found: " + fileName, ex);
        }
    }

    /**
     * Xóa file
     */
    public void deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
            log.info("File deleted successfully: {}", fileName);
        } catch (IOException ex) {
            log.error("Could not delete file: {}", fileName, ex);
            throw new FileStorageException("Could not delete file: " + fileName, ex);
        }
    }

    /**
     * Lấy URL đầy đủ của file
     */
    public String getFileUrl(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        return fileStorageProperties.getBaseUrl() + "/api/v1/files/" + fileName;
    }

    /**
     * Extract file name từ URL
     */
    public String extractFileNameFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Validate file upload
     */
    private void validateFile(MultipartFile file) {
        // Kiểm tra file empty
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot upload empty file");
        }

        // Kiểm tra kích thước file
        if (file.getSize() > fileStorageProperties.getMaxFileSize()) {
            throw new FileStorageException(
                    String.format("File size exceeds maximum limit of %d MB",
                            fileStorageProperties.getMaxFileSize() / (1024 * 1024))
            );
        }

        // Kiểm tra extension
        String fileName = file.getOriginalFilename();
        String extension = getFileExtension(fileName);

        boolean isAllowedExtension = Arrays.stream(fileStorageProperties.getAllowedExtensions())
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));

        if (!isAllowedExtension) {
            throw new FileStorageException(
                    String.format("File type not allowed. Allowed types: %s",
                            String.join(", ", fileStorageProperties.getAllowedExtensions()))
            );
        }

        // Validate là image file thật
        validateImageFile(file);
    }

    /**
     * Validate file là image hợp lệ
     */
    private void validateImageFile(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new FileStorageException("File is not a valid image");
            }
            log.debug("Image validated - Width: {}, Height: {}", image.getWidth(), image.getHeight());
        } catch (IOException ex) {
            throw new FileStorageException("Could not validate image file", ex);
        }
    }

    /**
     * Lấy file extension
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new FileStorageException("File must have an extension");
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Tạo tên file unique
     */
    private String generateUniqueFileName() {
        return UUID.randomUUID().toString();
    }
}