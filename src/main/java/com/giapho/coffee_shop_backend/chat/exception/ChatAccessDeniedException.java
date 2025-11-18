package com.giapho.coffee_shop_backend.chat.exception;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Ném ra khi người dùng không có quyền thao tác với cuộc trò chuyện hoặc tin nhắn.
 */
public class ChatAccessDeniedException extends BusinessException {

    public ChatAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác với cuộc trò chuyện này");
    }

    public ChatAccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
