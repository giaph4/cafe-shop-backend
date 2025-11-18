package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an action is not allowed due to the assignment workflow state.
 */
public class ShiftAssignmentStateException extends BusinessException {

    public ShiftAssignmentStateException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
