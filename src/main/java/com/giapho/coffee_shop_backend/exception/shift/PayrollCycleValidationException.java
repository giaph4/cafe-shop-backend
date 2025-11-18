package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when payroll cycle data is invalid.
 */
public class PayrollCycleValidationException extends BusinessException {

    public PayrollCycleValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
