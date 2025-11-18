package com.giapho.coffee_shop_backend.exception.supplier;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class SupplierValidationException extends BusinessException {

    public SupplierValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
