package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a shift instance cannot be located.
 */
public class ShiftNotFoundException extends BusinessException {

    public ShiftNotFoundException(Long shiftId) {
        super(HttpStatus.NOT_FOUND, String.format("Shift not found with id: %d", shiftId));
    }
}
