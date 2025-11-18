package com.giapho.coffee_shop_backend.exception.shift;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a shift template cannot be found.
 */
public class ShiftTemplateNotFoundException extends BusinessException {

    public ShiftTemplateNotFoundException(Long templateId) {
        super(HttpStatus.NOT_FOUND, String.format("Shift template not found with id: %d", templateId));
    }
}
