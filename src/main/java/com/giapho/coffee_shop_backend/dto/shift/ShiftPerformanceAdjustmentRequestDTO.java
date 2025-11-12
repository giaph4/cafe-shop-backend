package com.giapho.coffee_shop_backend.dto.shift;

import com.giapho.coffee_shop_backend.domain.enums.AdjustmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ShiftPerformanceAdjustmentRequestDTO(
        @NotNull(message = "Assignment ID không được để trống")
        Long assignmentId,
        @NotNull(message = "Loại điều chỉnh không được để trống")
        AdjustmentType type,
        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "0.0", inclusive = false, message = "Số tiền phải lớn hơn 0")
        BigDecimal amount,
        String reason
) {
}
