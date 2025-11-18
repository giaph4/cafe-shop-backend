package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a shift instance cannot transition due to its current state.
 */
public class ShiftInstanceStateException extends BusinessException {

    public ShiftInstanceStateException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
