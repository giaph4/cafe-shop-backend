package com.giapho.coffee_shop_backend.chat.exception;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class MessageAccessDeniedException extends BusinessException {

    public MessageAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác với tin nhắn này");
    }

    public MessageAccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
