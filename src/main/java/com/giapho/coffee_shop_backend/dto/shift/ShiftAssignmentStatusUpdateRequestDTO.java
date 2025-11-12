package com.giapho.coffee_shop_backend.dto.shift;

import com.giapho.coffee_shop_backend.domain.enums.ShiftAssignmentStatus;
import jakarta.validation.constraints.NotNull;

public record ShiftAssignmentStatusUpdateRequestDTO(
        @NotNull(message = "Trạng thái không được để trống")
        ShiftAssignmentStatus status,
        String notes
) {
}
