package com.giapho.coffee_shop_backend.exception.shift;

public class ShiftReportNotFoundException extends RuntimeException {

    public ShiftReportNotFoundException(Long sessionId) {
        super("Không tìm thấy báo cáo cho phiên ca " + sessionId);
    }
}
