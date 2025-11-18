package com.giapho.coffee_shop_backend.service.report.helper;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hỗ trợ xử lý dữ liệu chuỗi thời gian cho các báo cáo.
 */
public final class ReportTimeSeriesHelper {

    private ReportTimeSeriesHelper() {
    }

    public static <T> Map<LocalDate, T> newDateOrderedMap() {
        return new LinkedHashMap<>();
    }

    public static <T> void fillMissingDates(Map<LocalDate, T> map, LocalDate startDate, LocalDate endDate, T defaultValue) {
        LocalDate pointer = startDate;
        while (!pointer.isAfter(endDate)) {
            map.putIfAbsent(pointer, defaultValue);
            pointer = pointer.plusDays(1);
        }
    }
}
