package com.giapho.coffee_shop_backend.exception.voucher;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class VoucherConflictException extends BusinessException {

    public VoucherConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
