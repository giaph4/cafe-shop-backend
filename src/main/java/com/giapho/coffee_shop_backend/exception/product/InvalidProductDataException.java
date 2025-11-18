package com.giapho.coffee_shop_backend.exception.product;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidProductDataException extends BusinessException {

    public InvalidProductDataException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
