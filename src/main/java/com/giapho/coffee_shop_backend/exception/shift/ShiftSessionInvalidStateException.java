package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ShiftSessionInvalidStateException extends BusinessException {

    public ShiftSessionInvalidStateException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
