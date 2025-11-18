package com.giapho.coffee_shop_backend.exception.dashboard;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class DashboardUserNotFoundException extends BusinessException {

    private static final String MESSAGE = "Không xác định được người dùng hiện tại";

    public DashboardUserNotFoundException() {
        super(HttpStatus.NOT_FOUND, MESSAGE);
    }

    public DashboardUserNotFoundException(Long userId) {
        super(HttpStatus.NOT_FOUND, String.format("Không tìm thấy người dùng với id: %d", userId));
    }
}
