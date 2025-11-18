package com.giapho.coffee_shop_backend.exception.product;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to use a product that is marked as unavailable.
 */
public class ProductUnavailableException extends BusinessException {

    public ProductUnavailableException(Long productId) {
        super(HttpStatus.BAD_REQUEST, String.format("Product %d is currently unavailable", productId));
    }
}
