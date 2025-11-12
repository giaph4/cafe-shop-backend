package com.giapho.coffee_shop_backend.dto.shift;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ShiftAssignmentUpdateRequestDTO(
        @NotNull(message = "Assignment ID không được để trống")
        Long assignmentId,
        LocalTime plannedStart,
        LocalTime plannedEnd,
        @Min(value = 15, message = "Thời lượng ca tối thiểu 15 phút")
        Integer plannedMinutes,
        BigDecimal hourlyRate,
        BigDecimal fixedAllowance,
        String notes
) {
}
