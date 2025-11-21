package com.giapho.coffee_shop_backend.exception.audit;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidAuditLogException extends BusinessException {

    public InvalidAuditLogException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
