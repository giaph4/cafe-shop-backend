package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an adjustment action is not allowed in current state.
 */
public class ShiftPerformanceAdjustmentStateException extends BusinessException {

    public ShiftPerformanceAdjustmentStateException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
