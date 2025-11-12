package com.giapho.coffee_shop_backend.dto.shift;

import com.giapho.coffee_shop_backend.domain.enums.ShiftStatus;
import jakarta.validation.constraints.NotNull;

public record ShiftInstanceStatusUpdateRequestDTO(
        @NotNull(message = "Trạng thái ca không được để trống")
        ShiftStatus status,
        String notes
) {
}
