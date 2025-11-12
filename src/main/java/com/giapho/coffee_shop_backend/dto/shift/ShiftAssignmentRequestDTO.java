package com.giapho.coffee_shop_backend.dto.shift;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ShiftAssignmentRequestDTO(
        @NotNull(message = "Shift ID không được để trống")
        Long shiftId,
        @NotNull(message = "User ID không được để trống")
        Long userId,
        String roleName,
        @NotNull(message = "Thời gian bắt đầu không được để trống")
        LocalTime plannedStart,
        @NotNull(message = "Thời gian kết thúc không được để trống")
        LocalTime plannedEnd,
        @Min(value = 15, message = "Thời lượng ca tối thiểu 15 phút")
        Integer plannedMinutes,
        BigDecimal hourlyRate,
        BigDecimal fixedAllowance,
        String notes
) {
}
