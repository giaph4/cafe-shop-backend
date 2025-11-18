package com.giapho.coffee_shop_backend.exception.user;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown khi không tìm thấy người dùng theo tiêu chí truy vấn.
 */
public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, String.format("User not found with id: %d", id));
    }

    public UserNotFoundException(String username) {
        super(HttpStatus.NOT_FOUND, String.format("User not found with username: %s", username));
    }
}
