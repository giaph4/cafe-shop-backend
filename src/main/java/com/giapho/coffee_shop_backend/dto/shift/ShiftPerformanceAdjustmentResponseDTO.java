package com.giapho.coffee_shop_backend.dto.shift;

import com.giapho.coffee_shop_backend.domain.enums.AdjustmentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShiftPerformanceAdjustmentResponseDTO(
        Long id,
        Long assignmentId,
        AdjustmentType type,
        BigDecimal amount,
        String reason,
        LocalDateTime effectiveAt,
        boolean revoked,
        LocalDateTime revokedAt,
        String revokedBy,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
