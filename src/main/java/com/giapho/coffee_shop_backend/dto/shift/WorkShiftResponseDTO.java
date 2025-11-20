package com.giapho.coffee_shop_backend.dto.shift;

import java.time.LocalDateTime;

public record WorkShiftResponseDTO(
        Long id,
        String name,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Integer maxEmployees,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
