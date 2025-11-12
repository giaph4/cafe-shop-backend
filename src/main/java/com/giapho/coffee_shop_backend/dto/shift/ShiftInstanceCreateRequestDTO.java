package com.giapho.coffee_shop_backend.dto.shift;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ShiftInstanceCreateRequestDTO(
        @NotNull(message = "Template ID không được để trống")
        Long templateId,
        List<LocalDate> dates,
        LocalDate shiftDate,
        LocalTime startTime,
        LocalTime endTime,
        String notes
) {
}
