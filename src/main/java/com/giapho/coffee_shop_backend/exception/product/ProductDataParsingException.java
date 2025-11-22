package com.giapho.coffee_shop_backend.exception.product;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when product data cannot be parsed or is in invalid format.
 */
public class ProductDataParsingException extends BusinessException {

    public ProductDataParsingException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public ProductDataParsingException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, message, cause);
    }
}
