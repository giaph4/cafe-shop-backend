package com.giapho.coffee_shop_backend.exception.dashboard;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class DashboardInvalidDateRangeException extends BusinessException {

    public DashboardInvalidDateRangeException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
