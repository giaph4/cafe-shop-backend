package com.giapho.coffee_shop_backend.exception.user;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown khi username đã tồn tại trong hệ thống.
 */
public class UserUsernameAlreadyExistsException extends BusinessException {

    public UserUsernameAlreadyExistsException(String username) {
        super(HttpStatus.CONFLICT, String.format("Username already exists: %s", username));
    }
}
