package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ShiftAccessDeniedException extends BusinessException {

    public ShiftAccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
