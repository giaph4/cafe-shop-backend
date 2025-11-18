package com.giapho.coffee_shop_backend.chat.exception;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class MessageNotFoundException extends BusinessException {

    public MessageNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, String.format("Không tìm thấy tin nhắn với id: %d", id));
    }
}
