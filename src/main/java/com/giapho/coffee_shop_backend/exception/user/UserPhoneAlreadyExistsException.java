package com.giapho.coffee_shop_backend.exception.user;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown khi số điện thoại đã được sử dụng bởi tài khoản khác.
 */
public class UserPhoneAlreadyExistsException extends BusinessException {

    public UserPhoneAlreadyExistsException(String phone) {
        super(HttpStatus.CONFLICT, String.format("Phone number already exists: %s", phone));
    }
}
