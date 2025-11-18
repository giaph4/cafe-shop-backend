package com.giapho.coffee_shop_backend.exception.product;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a product cannot be found by the provided identifier.
 */
public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(Long productId) {
        super(HttpStatus.NOT_FOUND, String.format("Product not found with id: %d", productId));
    }
}
