package com.giapho.coffee_shop_backend.exception.order;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a payment method is missing or unsupported.
 */
public class PaymentMethodInvalidException extends BusinessException {

    public PaymentMethodInvalidException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
