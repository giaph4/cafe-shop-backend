package com.giapho.coffee_shop_backend.exception.user;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Ném ra khi mật khẩu mới trùng với mật khẩu cũ.
 */
public class UserPasswordUnchangedException extends BusinessException {

    public UserPasswordUnchangedException() {
        super(HttpStatus.BAD_REQUEST, "New password cannot be the same as the old password");
    }
}
