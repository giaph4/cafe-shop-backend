package com.giapho.coffee_shop_backend.exception.customer;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to delete a customer that still has associated orders.
 */
public class CustomerDeletionNotAllowedException extends BusinessException {

    public CustomerDeletionNotAllowedException(Long customerId) {
        super(HttpStatus.BAD_REQUEST,
                String.format("Cannot delete customer %d because purchase history exists", customerId));
    }
}
