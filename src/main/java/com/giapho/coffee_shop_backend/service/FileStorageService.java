package com.giapho.coffee_shop_backend.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Hợp đồng lưu trữ và truy xuất tệp tin.
 */
public interface FileStorageService {

    /**
     * Lưu một tệp và trả về tên file mới trong hệ thống.
     *
     * @param file tệp đầu vào
     * @return tên file đã được chuẩn hóa
     */
    String storeFile(MultipartFile file);

    /**
     * Tải nội dung file dưới dạng {@link Resource}.
     *
     * @param fileName tên file cần tải
     * @return resource đại diện file
     */
    Resource loadFileAsResource(String fileName);

    /**
     * Xóa file khỏi kho lưu trữ nếu tồn tại.
     *
     * @param fileName tên file cần xóa
     */
    void deleteFile(String fileName);

    /**
     * Tạo URL truy cập công khai cho file.
     *
     * @param fileName tên file
     * @return đường dẫn đầy đủ hoặc null nếu không hợp lệ
     */
    String getFileUrl(String fileName);

    /**
     * Trích xuất tên file từ URL do hệ thống cấp.
     *
     * @param fileUrl đường dẫn file
     * @return tên file hoặc null nếu không hợp lệ
     */
    String extractFileNameFromUrl(String fileUrl);
}