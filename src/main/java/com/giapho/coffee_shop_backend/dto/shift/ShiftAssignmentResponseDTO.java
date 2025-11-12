package com.giapho.coffee_shop_backend.dto.shift;

import com.giapho.coffee_shop_backend.domain.enums.ShiftAssignmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record ShiftAssignmentResponseDTO(
        Long id,
        Long shiftId,
        Long userId,
        String username,
        String fullName,
        String roleName,
        LocalTime plannedStart,
        LocalTime plannedEnd,
        Integer plannedMinutes,
        Integer actualMinutes,
        Integer totalOrders,
        BigDecimal totalRevenue,
        BigDecimal hourlyRate,
        BigDecimal fixedAllowance,
        BigDecimal bonusAmount,
        BigDecimal penaltyAmount,
        ShiftAssignmentStatus status,
        String notes,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AttendanceRecordResponseDTO> attendanceRecords,
        List<ShiftPerformanceAdjustmentResponseDTO> adjustments
) {
}
