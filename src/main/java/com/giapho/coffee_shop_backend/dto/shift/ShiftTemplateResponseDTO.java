package com.giapho.coffee_shop_backend.dto.shift;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

public record ShiftTemplateResponseDTO(
        Long id,
        String name,
        String description,
        LocalTime startTime,
        LocalTime endTime,
        Set<String> requiredRoles,
        BigDecimal defaultHourlyRate,
        BigDecimal defaultFixedAllowance,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
