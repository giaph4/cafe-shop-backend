package com.giapho.coffee_shop_backend.exception.login;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class LoginHistoryInvalidDateRangeException extends BusinessException {

    public LoginHistoryInvalidDateRangeException() {
        super(HttpStatus.BAD_REQUEST, "Thời gian bắt đầu phải nhỏ hơn hoặc bằng thời gian kết thúc.");
    }
}
