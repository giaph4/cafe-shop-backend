package com.giapho.coffee_shop_backend.exception.authentication;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Ném ra khi tài khoản bị khóa hoặc vô hiệu hóa.
 */
public class AuthenticationAccountDisabledException extends BusinessException {

    public AuthenticationAccountDisabledException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
