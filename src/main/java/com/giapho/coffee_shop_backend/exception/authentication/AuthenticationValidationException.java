package com.giapho.coffee_shop_backend.exception.authentication;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Ném ra khi dữ liệu đăng nhập/đăng ký không hợp lệ.
 */
public class AuthenticationValidationException extends BusinessException {

    public AuthenticationValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
