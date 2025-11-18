package com.giapho.coffee_shop_backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for domain-specific business exceptions with HTTP status mapping.
 */
public abstract class BusinessException extends RuntimeException {

    private final HttpStatus status;

    protected BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
