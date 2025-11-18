package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a shift performance adjustment cannot be found.
 */
public class ShiftPerformanceAdjustmentNotFoundException extends BusinessException {

    public ShiftPerformanceAdjustmentNotFoundException(Long adjustmentId) {
        super(HttpStatus.NOT_FOUND,
                String.format("Shift performance adjustment not found with id: %d", adjustmentId));
    }
}
