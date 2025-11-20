package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ShiftSessionLimitReachedException extends BusinessException {

    public ShiftSessionLimitReachedException(Long shiftId) {
        super(HttpStatus.CONFLICT, "Shift has reached maximum active employees (shiftId=" + shiftId + ")");
    }
}
