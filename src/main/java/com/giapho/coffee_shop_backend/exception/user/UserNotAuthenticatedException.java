package com.giapho.coffee_shop_backend.exception.user;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Ném ra khi chưa xác định được người dùng hiện tại trong context bảo mật.
 */
public class UserNotAuthenticatedException extends BusinessException {

    public UserNotAuthenticatedException() {
        super(HttpStatus.UNAUTHORIZED, "User not authenticated");
    }
}
