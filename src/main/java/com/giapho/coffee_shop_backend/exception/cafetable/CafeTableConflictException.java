package com.giapho.coffee_shop_backend.exception.cafetable;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Ném ra khi tên bàn bị trùng với bàn khác.
 */
public class CafeTableConflictException extends BusinessException {

    public CafeTableConflictException(String name) {
        super(HttpStatus.CONFLICT, "Tên bàn đã tồn tại: " + name);
    }
}
