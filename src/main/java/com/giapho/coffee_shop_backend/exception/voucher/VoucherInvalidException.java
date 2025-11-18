package com.giapho.coffee_shop_backend.exception.voucher;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when applying an invalid or inactive voucher to an order.
 */
public class VoucherInvalidException extends BusinessException {

    public VoucherInvalidException(String code, String reason) {
        super(HttpStatus.BAD_REQUEST,
                String.format("Voucher %s is invalid: %s", code, reason));
    }

    public VoucherInvalidException(String reason) {
        super(HttpStatus.BAD_REQUEST, reason);
    }
}
