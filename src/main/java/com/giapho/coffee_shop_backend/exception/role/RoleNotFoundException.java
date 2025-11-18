package com.giapho.coffee_shop_backend.exception.role;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown khi không tìm thấy vai trò theo ID cung cấp.
 */
public class RoleNotFoundException extends BusinessException {

    public RoleNotFoundException(Long roleId) {
        super(HttpStatus.NOT_FOUND, buildMessage(roleId));
    }

    private static String buildMessage(Long roleId) {
        if (roleId == null) {
            return "Role not found";
        }
        return String.format("Role not found with id: %d", roleId);
    }
}
