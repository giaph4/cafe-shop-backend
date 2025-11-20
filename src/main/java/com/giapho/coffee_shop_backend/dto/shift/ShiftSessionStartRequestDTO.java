package com.giapho.coffee_shop_backend.dto.shift;

import jakarta.validation.constraints.NotNull;

public record ShiftSessionStartRequestDTO(
        @NotNull(message = "Mã ca làm việc không được để trống")
        Long workShiftId,
        Boolean adminOverride
) {
}
