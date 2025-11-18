package com.giapho.coffee_shop_backend.exception.supplier;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class SupplierConflictException extends BusinessException {

    public SupplierConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
