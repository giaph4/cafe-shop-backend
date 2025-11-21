package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.analytics.DashboardMetricsDTO;

import java.time.LocalDate;

public interface DashboardAnalyticsService {

    DashboardMetricsDTO collectMetrics(LocalDate from, LocalDate to,
                                       boolean includeTopProducts,
                                       boolean includeVoucherStats,
                                       boolean includeCustomerStats);
}
