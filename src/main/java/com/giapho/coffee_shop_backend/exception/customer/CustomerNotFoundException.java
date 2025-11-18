package com.giapho.coffee_shop_backend.exception.customer;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a customer cannot be found using provided identifiers.
 */
public class CustomerNotFoundException extends BusinessException {

    public CustomerNotFoundException(Long customerId) {
        super(HttpStatus.NOT_FOUND, String.format("Customer not found with id: %d", customerId));
    }

    public CustomerNotFoundException(String fieldName, Object fieldValue) {
        super(HttpStatus.NOT_FOUND,
                String.format("Customer not found with %s: %s", fieldName, fieldValue));
    }
}
