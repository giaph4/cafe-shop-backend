package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a shift assignment cannot be found.
 */
public class ShiftAssignmentNotFoundException extends BusinessException {

    public ShiftAssignmentNotFoundException(Long assignmentId) {
        super(HttpStatus.NOT_FOUND, String.format("Shift assignment not found with id: %d", assignmentId));
    }
}
