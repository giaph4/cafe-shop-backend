package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ShiftSessionNotFoundException extends BusinessException {

    public ShiftSessionNotFoundException(Long sessionId) {
        super(HttpStatus.NOT_FOUND, "Shift session not found with id: " + sessionId);
    }

    public ShiftSessionNotFoundException() {
        super(HttpStatus.NOT_FOUND, "No active shift session found");
    }
}
