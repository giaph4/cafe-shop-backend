package com.giapho.coffee_shop_backend.dto.shift;

import com.giapho.coffee_shop_backend.domain.enums.PayrollCycleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PayrollCycleRequestDTO(
        Long id,
        @NotBlank @Size(max = 50) String code,
        @Size(max = 255) String name,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        PayrollCycleStatus status,
        String notes
) {
}
