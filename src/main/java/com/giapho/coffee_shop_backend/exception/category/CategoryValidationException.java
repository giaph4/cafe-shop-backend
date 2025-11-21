package com.giapho.coffee_shop_backend.exception.category;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CategoryValidationException extends BusinessException {

    public CategoryValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
