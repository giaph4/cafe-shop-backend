package com.giapho.coffee_shop_backend.exception;

/**
 * Exception cho các lỗi liên quan đến file storage
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}