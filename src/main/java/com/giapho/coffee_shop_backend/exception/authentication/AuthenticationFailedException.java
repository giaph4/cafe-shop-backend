package com.giapho.coffee_shop_backend.exception.authentication;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Ném ra khi quá trình xác thực thất bại vì thông tin đăng nhập không hợp lệ.
 */
public class AuthenticationFailedException extends BusinessException {

    public AuthenticationFailedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
