package com.giapho.coffee_shop_backend.exception.product;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to delete a product that has historical usage.
 */
public class ProductDeletionNotAllowedException extends BusinessException {

    public ProductDeletionNotAllowedException(String productName) {
        super(HttpStatus.BAD_REQUEST, String.format(
                "Cannot delete product '%s' because it is referenced by existing orders", productName));
    }
}
