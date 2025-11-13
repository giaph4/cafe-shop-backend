package com.giapho.coffee_shop_backend.dto.dashboard;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record ManagerDashboardDTO(
        ShiftOverview shiftOverview,
        TeamPerformance teamPerformance,
        InventoryFocus inventory,
        PayrollOverview payroll,
        List<PendingApproval> pendingApprovals,
        List<AttendanceAlert> attendanceAlerts,
        List<ServiceIssue> serviceIssues
) {

    @Builder
    public record ShiftOverview(
            int scheduledToday,
            int inProgress,
            int completed,
            int cancelled,
            List<ShiftCard> upcomingShifts
    ) {
    }

    @Builder
    public record ShiftCard(
            Long shiftId,
            LocalDate shiftDate,
            String timeRange,
            String status,
            int assignedStaff,
            int capacity
    ) {
    }

    @Builder
    public record TeamPerformance(
            BigDecimal totalRevenue,
            int totalOrders,
            BigDecimal averageOrderValue,
            List<StaffLeaderboardItem> topStaff
    ) {
    }

    @Builder
    public record StaffLeaderboardItem(
            Long staffId,
            String staffName,
            long orders,
            BigDecimal revenue,
            BigDecimal averageOrderValue
    ) {
    }

    @Builder
    public record InventoryFocus(
            int lowStockItems,
            int criticalStockItems,
            List<InventoryAlert> alerts
    ) {
    }

    @Builder
    public record InventoryAlert(
            Long ingredientId,
            String ingredientName,
            BigDecimal quantityOnHand,
            BigDecimal reorderLevel
    ) {
    }

    @Builder
    public record PayrollOverview(
            BigDecimal estimatedPayroll,
            BigDecimal bonusTotal,
            BigDecimal penaltyTotal,
            BigDecimal adjustmentNet,
            int staffCount
    ) {
    }

    @Builder
    public record PendingApproval(
            String module,
            String description,
            String requestedBy,
            LocalDate requestedAt,
            String status
    ) {
    }

    @Builder
    public record AttendanceAlert(
            Long assignmentId,
            Long staffId,
            String staffName,
            String issueType,
            String note
    ) {
    }

    @Builder
    public record ServiceIssue(
            Long orderId,
            String tableName,
            String issue,
            String severity,
            LocalDate createdDate
    ) {
    }
}
