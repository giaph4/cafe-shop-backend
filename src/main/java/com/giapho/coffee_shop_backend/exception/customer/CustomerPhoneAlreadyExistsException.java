package com.giapho.coffee_shop_backend.exception.customer;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to create or update a customer with a duplicated phone number.
 */
public class CustomerPhoneAlreadyExistsException extends BusinessException {

    public CustomerPhoneAlreadyExistsException(String phone) {
        super(HttpStatus.CONFLICT, String.format("Phone number already exists: %s", phone));
    }
}
