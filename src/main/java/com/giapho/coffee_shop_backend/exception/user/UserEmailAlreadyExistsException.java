package com.giapho.coffee_shop_backend.exception.user;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown khi email đã tồn tại trong hệ thống.
 */
public class UserEmailAlreadyExistsException extends BusinessException {

    public UserEmailAlreadyExistsException(String email) {
        super(HttpStatus.CONFLICT, String.format("Email already exists: %s", email));
    }
}
