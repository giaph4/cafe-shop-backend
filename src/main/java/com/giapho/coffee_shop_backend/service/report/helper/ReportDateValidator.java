package com.giapho.coffee_shop_backend.service.report.helper;

import com.giapho.coffee_shop_backend.exception.report.ReportValidationException;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Tiện ích chuẩn hoá kiểm tra dữ liệu thời gian đầu vào cho các báo cáo.
 */
public final class ReportDateValidator {

    private ReportDateValidator() {
    }

    public static void validateMandatoryRange(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "startDate không được để trống");
        Objects.requireNonNull(endDate, "endDate không được để trống");
        ensureStartBeforeOrEqualEnd(startDate, endDate);
    }

    public static void validateOptionalRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            ensureStartBeforeOrEqualEnd(startDate, endDate);
        }
    }

    private static void ensureStartBeforeOrEqualEnd(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new ReportValidationException("Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu");
        }
    }
}
