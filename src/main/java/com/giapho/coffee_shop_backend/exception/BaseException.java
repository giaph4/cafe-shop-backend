package com.giapho.coffee_shop_backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception class for the application
 */
@Getter
public class BaseException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public BaseException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public BaseException(HttpStatus status, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }
}
