package com.giapho.coffee_shop_backend.dto.shift;

import jakarta.validation.constraints.NotBlank;

public record ShiftPerformanceAdjustmentRevokeRequestDTO(
        @NotBlank(message = "Lý do thu hồi không được để trống")
        String reason
) {
}
