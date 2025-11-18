package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a shift instance cannot be found.
 */
public class ShiftInstanceNotFoundException extends BusinessException {

    public ShiftInstanceNotFoundException(Long shiftInstanceId) {
        super(HttpStatus.NOT_FOUND, String.format("Shift instance not found with id: %d", shiftInstanceId));
    }
}
