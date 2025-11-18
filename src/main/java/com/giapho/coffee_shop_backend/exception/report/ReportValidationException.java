package com.giapho.coffee_shop_backend.exception.report;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Xuất hiện khi dữ liệu đầu vào của báo cáo không hợp lệ (ví dụ khoảng ngày).
 */
public class ReportValidationException extends BusinessException {

    public ReportValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
