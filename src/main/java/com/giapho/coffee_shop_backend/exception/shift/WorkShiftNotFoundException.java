package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class WorkShiftNotFoundException extends BusinessException {

    public WorkShiftNotFoundException(Long shiftId) {
        super(HttpStatus.NOT_FOUND, "Work shift not found with id: " + shiftId);
    }
}
