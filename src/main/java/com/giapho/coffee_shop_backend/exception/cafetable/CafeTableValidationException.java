package com.giapho.coffee_shop_backend.exception.cafetable;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Ném ra khi dữ liệu yêu cầu bàn không hợp lệ.
 */
public class CafeTableValidationException extends BusinessException {

    public CafeTableValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
