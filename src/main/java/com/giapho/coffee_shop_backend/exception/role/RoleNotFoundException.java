package com.giapho.coffee_shop_backend.exception.role;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class RoleNotFoundException extends BusinessException {

    public RoleNotFoundException(Long roleId) {
        this("id", roleId != null ? String.valueOf(roleId) : null);
    }

    public RoleNotFoundException(String fieldName, String fieldValue) {
        super(HttpStatus.NOT_FOUND, buildMessage(fieldName, fieldValue));
    }

    private static String buildMessage(String fieldName, String fieldValue) {
        if (fieldName == null && fieldValue == null) {
            return "Role not found";
        }
        if (fieldValue == null || fieldValue.isBlank()) {
            return String.format("Role not found with %s", fieldName);
        }
        return String.format("Role not found with %s: %s", fieldName, fieldValue);
    }
}
