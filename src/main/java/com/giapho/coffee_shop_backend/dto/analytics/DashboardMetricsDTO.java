package com.giapho.coffee_shop_backend.dto.analytics;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record DashboardMetricsDTO(
        LocalDate from,
        LocalDate to,
        long totalOrders,
        long paidOrders,
        long cancelledOrders,
        BigDecimal totalRevenue,
        BigDecimal averageOrderValue,
        BigDecimal totalDiscount,
        long voucherUsageCount,
        List<BestSellerMetric> topProducts,
        List<CustomerMetric> topCustomers,
        List<StaffMetric> topStaff
) {
    @Builder
    public record BestSellerMetric(
            Long productId,
            String productName,
            long totalQuantity,
            BigDecimal totalRevenue
    ) {
    }

    @Builder
    public record CustomerMetric(
            Long customerId,
            String customerName,
            String phone,
            long orderCount,
            BigDecimal totalSpend,
            BigDecimal averageSpend
    ) {
    }

    @Builder
    public record StaffMetric(
            Long staffId,
            String staffName,
            long orderCount,
            BigDecimal totalRevenue
    ) {
    }
}
