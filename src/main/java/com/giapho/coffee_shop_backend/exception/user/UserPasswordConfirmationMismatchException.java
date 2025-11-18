package com.giapho.coffee_shop_backend.exception.user;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Ném ra khi mật khẩu mới và mật khẩu xác nhận không khớp.
 */
public class UserPasswordConfirmationMismatchException extends BusinessException {

    public UserPasswordConfirmationMismatchException() {
        super(HttpStatus.BAD_REQUEST, "New password and confirmation password do not match");
    }
}
