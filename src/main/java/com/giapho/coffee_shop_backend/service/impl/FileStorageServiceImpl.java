package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.config.FileStorageProperties;
import com.giapho.coffee_shop_backend.exception.FileStorageException;
import com.giapho.coffee_shop_backend.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private final Path storageRoot;
    private final FileStorageProperties properties;

    public FileStorageServiceImpl(FileStorageProperties properties) {
        this.properties = properties;
        this.storageRoot = initializeStorage(properties.getUploadDir());
    }

    @Override
    public String storeFile(MultipartFile file) {
        validateFile(file);

        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = extractExtension(originalName);
        String storedName = generateStoredName(extension);

        copyToStorage(file, storedName, originalName);
        return storedName;
    }

    @Override
    public Resource loadFileAsResource(String fileName) {
        Path filePath = resolvePath(fileName);
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
        } catch (MalformedURLException ex) {
            log.warn("Invalid file URL for {}", fileName, ex);
        }
        throw new FileStorageException("File not found: " + fileName);
    }

    @Override
    public void deleteFile(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return;
        }
        Path filePath = resolvePath(fileName);
        try {
            Files.deleteIfExists(filePath);
            log.info("Deleted file {}", fileName);
        } catch (IOException ex) {
            throw new FileStorageException("Could not delete file: " + fileName, ex);
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return null;
        }
        return properties.getBaseUrl() + "/api/v1/files/" + fileName;
    }

    @Override
    public String extractFileNameFromUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return null;
        }
        int lastSlash = fileUrl.lastIndexOf('/') + 1;
        return lastSlash > 0 && lastSlash < fileUrl.length() ? fileUrl.substring(lastSlash) : null;
    }

    private Path initializeStorage(String uploadDir) {
        try {
            Path location = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(location);
            log.info("File storage directory prepared at {}", location);
            return location;
        } catch (IOException ex) {
            throw new FileStorageException("Could not initialize file storage directory", ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Cannot upload empty file");
        }

        if (file.getSize() > properties.getMaxFileSize()) {
            throw new FileStorageException(String.format(
                    "File size exceeds maximum limit of %d MB",
                    properties.getMaxFileSize() / (1024 * 1024)
            ));
        }

        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        ensureAllowedExtension(extension);

        if (isImageExtension(extension)) {
            validateImage(file);
        }
    }

    private void ensureAllowedExtension(String extension) {
        boolean allowed = Arrays.stream(properties.getAllowedExtensions())
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));
        if (!allowed) {
            throw new FileStorageException(String.format(
                    "File type '%s' is not allowed. Allowed types: %s",
                    extension,
                    String.join(", ", properties.getAllowedExtensions())
            ));
        }
    }

    private void validateImage(MultipartFile file) {
        try (InputStream stream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                throw new FileStorageException("Image file is corrupted or unsupported");
            }
            log.debug("Validated image {}x{}", image.getWidth(), image.getHeight());
        } catch (IOException ex) {
            throw new FileStorageException("Could not validate image file", ex);
        }
    }

    private void copyToStorage(MultipartFile file, String storedName, String originalName) {
        Path target = resolvePath(storedName);
        try {
            if (originalName.contains("..")) {
                throw new FileStorageException("Filename contains invalid path sequence: " + originalName);
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file {} (original: {})", storedName, originalName);
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file: " + storedName, ex);
        }
    }

    private Path resolvePath(String fileName) {
        return storageRoot.resolve(fileName).normalize();
    }

    private String extractExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            throw new FileStorageException("File must have an extension");
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private boolean isImageExtension(String extension) {
        return Arrays.stream(properties.getImageExtensions())
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));
    }

    private String generateStoredName(String extension) {
        return Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + "." + extension;
    }
}
