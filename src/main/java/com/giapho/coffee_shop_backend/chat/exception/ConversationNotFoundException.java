package com.giapho.coffee_shop_backend.chat.exception;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thông báo khi không tìm thấy cuộc trò chuyện.
 */
public class ConversationNotFoundException extends BusinessException {

    public ConversationNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, String.format("Không tìm thấy cuộc trò chuyện với id: %d", id));
    }
}
