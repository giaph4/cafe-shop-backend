package com.giapho.coffee_shop_backend.exception.product;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to create or update a product with a duplicate code.
 */
public class ProductCodeAlreadyExistsException extends BusinessException {

    public ProductCodeAlreadyExistsException(String productCode) {
        super(HttpStatus.BAD_REQUEST, String.format("Product code already exists: %s", productCode));
    }
}
