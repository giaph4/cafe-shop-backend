package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ShiftSessionAlreadyActiveException extends BusinessException {

    public ShiftSessionAlreadyActiveException(Long userId) {
        super(HttpStatus.CONFLICT, "User " + userId + " already has an active shift session");
    }
}
