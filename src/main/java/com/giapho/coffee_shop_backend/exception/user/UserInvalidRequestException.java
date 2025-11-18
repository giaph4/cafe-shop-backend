package com.giapho.coffee_shop_backend.exception.user;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Ném ra khi yêu cầu cập nhật người dùng chứa dữ liệu không hợp lệ.
 */
public class UserInvalidRequestException extends BusinessException {

    public UserInvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
