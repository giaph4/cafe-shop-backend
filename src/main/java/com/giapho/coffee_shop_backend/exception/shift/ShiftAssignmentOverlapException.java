package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when creating or updating an assignment conflicts with an existing shift in the same timeframe.
 */
public class ShiftAssignmentOverlapException extends BusinessException {

    public ShiftAssignmentOverlapException() {
        super(HttpStatus.CONFLICT, "Employee already has another assignment in this time range");
    }
}
