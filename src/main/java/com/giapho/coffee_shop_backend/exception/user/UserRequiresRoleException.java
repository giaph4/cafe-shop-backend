package com.giapho.coffee_shop_backend.exception.user;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown khi thao tác yêu cầu phải gán ít nhất một vai trò cho người dùng.
 */
public class UserRequiresRoleException extends BusinessException {

    public UserRequiresRoleException() {
        super(HttpStatus.BAD_REQUEST, "User must have at least one role");
    }
}
