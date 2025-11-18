package com.giapho.coffee_shop_backend.exception.report;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Được ném ra khi quá trình xuất báo cáo gặp lỗi hệ thống (ví dụ lỗi Excel).
 */
public class ReportExportException extends BusinessException {

    public ReportExportException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
        initCause(cause);
    }

    public ReportExportException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
