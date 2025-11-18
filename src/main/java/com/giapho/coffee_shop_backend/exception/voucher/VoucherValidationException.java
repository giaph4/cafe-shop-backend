package com.giapho.coffee_shop_backend.exception.voucher;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when voucher request payload violates validation rules.
 */
public class VoucherValidationException extends BusinessException {

    public VoucherValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
