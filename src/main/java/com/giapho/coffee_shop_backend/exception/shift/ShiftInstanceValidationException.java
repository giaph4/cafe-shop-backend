package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when shift instance data violates validation rules.
 */
public class ShiftInstanceValidationException extends BusinessException {

    public ShiftInstanceValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
