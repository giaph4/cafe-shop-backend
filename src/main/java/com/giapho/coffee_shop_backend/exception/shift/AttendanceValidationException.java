package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when attendance input or state is invalid for the requested action.
 */
public class AttendanceValidationException extends BusinessException {

    public AttendanceValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
