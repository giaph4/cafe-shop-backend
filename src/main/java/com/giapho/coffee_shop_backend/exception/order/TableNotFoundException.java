package com.giapho.coffee_shop_backend.exception.order;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a cafe table cannot be located.
 */
public class TableNotFoundException extends BusinessException {

    public TableNotFoundException(Long tableId) {
        super(HttpStatus.NOT_FOUND, String.format("Table not found with id: %d", tableId));
    }
}
