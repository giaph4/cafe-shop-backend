package com.giapho.coffee_shop_backend.dto.shift;

import com.giapho.coffee_shop_backend.domain.enums.ShiftStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record ShiftInstanceResponseDTO(
        Long id,
        Long templateId,
        String templateName,
        LocalDate shiftDate,
        LocalTime startTime,
        LocalTime endTime,
        ShiftStatus status,
        LocalDateTime lockedAt,
        String notes,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ShiftAssignmentResponseDTO> assignments
) {
}
