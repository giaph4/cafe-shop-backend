package com.giapho.coffee_shop_backend.exception;

/**
 * Ngoại lệ chuẩn cho các lỗi lưu trữ file.
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}