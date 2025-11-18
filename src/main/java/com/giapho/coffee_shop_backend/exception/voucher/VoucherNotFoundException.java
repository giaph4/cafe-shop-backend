package com.giapho.coffee_shop_backend.exception.voucher;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a voucher cannot be located by the provided identifier.
 */
public class VoucherNotFoundException extends BusinessException {

    public VoucherNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, String.format("Voucher không tồn tại: %d", id));
    }

    public VoucherNotFoundException(String code) {
        super(HttpStatus.NOT_FOUND, String.format("Voucher không tồn tại: %s", code));
    }
}
