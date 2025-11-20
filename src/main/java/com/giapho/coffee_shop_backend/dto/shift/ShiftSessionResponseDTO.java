package com.giapho.coffee_shop_backend.dto.shift;

import com.giapho.coffee_shop_backend.domain.enums.ShiftSessionStatus;

import java.time.LocalDateTime;

public record ShiftSessionResponseDTO(
        Long id,
        Long workShiftId,
        Long userId,
        String username,
        String fullName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        ShiftSessionStatus status,
        boolean adminOverride,
        String forceReason,
        Long forceByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
