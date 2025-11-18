package com.giapho.coffee_shop_backend.exception.inventory;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when inventory cannot satisfy the requested quantity.
 */
public class InsufficientInventoryException extends BusinessException {

    public InsufficientInventoryException(String ingredientName, String required, String available) {
        super(HttpStatus.BAD_REQUEST,
                String.format("Not enough stock for ingredient %s. Required: %s, Available: %s",
                        ingredientName,
                        required,
                        available));
    }
}
