package com.giapho.coffee_shop_backend.dto.dashboard;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record StaffDashboardDTO(
        PersonalShiftSummary shiftSummary,
        List<UpcomingShift> upcomingShifts,
        PerformanceSnapshot performance,
        AttendanceStatus attendance,
        PayrollSnapshot payroll,
        List<TaskReminder> taskReminders,
        List<Announcement> announcements
) {

    @Builder
    public record PersonalShiftSummary(
            int shiftsThisWeek,
            int completedShifts,
            int pendingShifts,
            int lateCheckIns,
            int earlyCheckOuts
    ) {
    }

    @Builder
    public record UpcomingShift(
            Long assignmentId,
            LocalDate shiftDate,
            String timeRange,
            String role,
            String status,
            String managerNote
    ) {
    }

    @Builder
    public record PerformanceSnapshot(
            BigDecimal totalRevenue,
            long totalOrders,
            BigDecimal averageOrderValue,
            long positiveFeedbacks,
            long negativeFeedbacks
    ) {
    }

    @Builder
    public record AttendanceStatus(
            boolean currentlyCheckedIn,
            LocalDateTime lastCheckIn,
            LocalDateTime lastCheckOut,
            int consecutiveOnTimeDays
    ) {
    }

    @Builder
    public record PayrollSnapshot(
            BigDecimal estimatedCurrentCycle,
            BigDecimal bonusTotal,
            BigDecimal penaltyTotal,
            BigDecimal adjustmentNet,
            BigDecimal lastCyclePaid
    ) {
    }

    @Builder
    public record TaskReminder(
            String title,
            String description,
            LocalDate dueDate,
            String priority
    ) {
    }

    @Builder
    public record Announcement(
            String title,
            String content,
            LocalDateTime publishedAt,
            String publisher
    ) {
    }
}
