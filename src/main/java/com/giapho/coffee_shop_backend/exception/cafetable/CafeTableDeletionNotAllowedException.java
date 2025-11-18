package com.giapho.coffee_shop_backend.exception.cafetable;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Ném ra khi cố gắng xoá bàn vẫn còn đơn hàng liên kết.
 */
public class CafeTableDeletionNotAllowedException extends BusinessException {

    public CafeTableDeletionNotAllowedException(Long tableId) {
        super(HttpStatus.CONFLICT, "Không thể xoá bàn có ID " + tableId + " vì vẫn còn đơn hàng liên quan");
    }
}
