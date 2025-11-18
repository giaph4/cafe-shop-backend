package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a shift template request violates validation rules.
 */
public class ShiftTemplateValidationException extends BusinessException {

    public ShiftTemplateValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
