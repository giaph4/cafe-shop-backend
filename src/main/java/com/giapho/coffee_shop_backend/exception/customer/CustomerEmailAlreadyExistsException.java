package com.giapho.coffee_shop_backend.exception.customer;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to use an email that already belongs to another customer.
 */
public class CustomerEmailAlreadyExistsException extends BusinessException {

    public CustomerEmailAlreadyExistsException(String email) {
        super(HttpStatus.CONFLICT, String.format("Email already exists: %s", email));
    }
}
