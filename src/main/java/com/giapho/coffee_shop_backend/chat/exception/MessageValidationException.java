package com.giapho.coffee_shop_backend.chat.exception;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class MessageValidationException extends BusinessException {

    public MessageValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
