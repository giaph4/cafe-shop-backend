package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a payroll cycle cannot be located.
 */
public class PayrollCycleNotFoundException extends BusinessException {

    public PayrollCycleNotFoundException(Long cycleId) {
        super(HttpStatus.NOT_FOUND, String.format("Payroll cycle not found with id: %d", cycleId));
    }
}
