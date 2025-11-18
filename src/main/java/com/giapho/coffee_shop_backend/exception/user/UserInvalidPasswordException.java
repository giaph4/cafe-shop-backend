package com.giapho.coffee_shop_backend.exception.user;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Ném ra khi mật khẩu hiện tại không chính xác.
 */
public class UserInvalidPasswordException extends BusinessException {

    public UserInvalidPasswordException() {
        super(HttpStatus.BAD_REQUEST, "Incorrect current password");
    }
}
