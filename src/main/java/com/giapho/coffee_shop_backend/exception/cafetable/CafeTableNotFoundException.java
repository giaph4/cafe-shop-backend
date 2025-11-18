package com.giapho.coffee_shop_backend.exception.cafetable;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Ném ra khi không tìm thấy bàn với định danh cung cấp.
 */
public class CafeTableNotFoundException extends BusinessException {

    public CafeTableNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Không tìm thấy bàn với mã: " + id);
    }
}
