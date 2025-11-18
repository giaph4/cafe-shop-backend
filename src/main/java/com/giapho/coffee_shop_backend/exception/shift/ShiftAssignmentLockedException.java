package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to modify an assignment in a locked or completed shift.
 */
public class ShiftAssignmentLockedException extends BusinessException {

    public ShiftAssignmentLockedException(Long shiftId) {
        super(HttpStatus.BAD_REQUEST,
                String.format("Shift %d is locked or completed and cannot be modified", shiftId));
    }
}
