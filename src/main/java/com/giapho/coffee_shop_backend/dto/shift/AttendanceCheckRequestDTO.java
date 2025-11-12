package com.giapho.coffee_shop_backend.dto.shift;

import com.giapho.coffee_shop_backend.domain.enums.AttendanceSource;
import jakarta.validation.constraints.NotNull;

public record AttendanceCheckRequestDTO(
        Long shiftId,
        Long assignmentId,
        Long userId,
        @NotNull(message = "Nguồn chấm công không được để trống")
        AttendanceSource source,
        String note
) {
}
