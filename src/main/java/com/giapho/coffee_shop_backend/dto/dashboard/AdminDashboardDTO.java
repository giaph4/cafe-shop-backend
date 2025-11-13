package com.giapho.coffee_shop_backend.dto.dashboard;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record AdminDashboardDTO(
        RevenueSnapshot revenue,
        OrderSnapshot orders,
        InventorySnapshot inventory,
        List<TopStaffMetric> topStaff,
        List<TopProductMetric> topProducts,
        List<TopCustomerMetric> topCustomers,
        List<SystemAlert> alerts
) {

    @Builder
    public record RevenueSnapshot(
            BigDecimal today,
            BigDecimal month,
            BigDecimal year,
            BigDecimal averageOrderValue,
            BigDecimal todayProfit,
            BigDecimal monthProfit
    ) {
    }

    @Builder
    public record OrderSnapshot(
            long today,
            long month,
            long year,
            long cancelledToday,
            long cancelledMonth
    ) {
    }

    @Builder
    public record InventorySnapshot(
            int lowStockItems,
            int totalSuppliers,
            int pendingPurchaseOrders
    ) {
    }

    @Builder
    public record TopProductMetric(
            Long productId,
            String productName,
            long quantity,
            BigDecimal revenue
    ) {
    }

    @Builder
    public record TopCustomerMetric(
            Long customerId,
            String customerName,
            String phone,
            long orders,
            BigDecimal spend
    ) {
    }

    @Builder
    public record TopStaffMetric(
            Long staffId,
            String staffName,
            long orders,
            BigDecimal revenue
    ) {
    }

    @Builder
    public record SystemAlert(
            String type,
            String message,
            String severity
    ) {
    }
}
