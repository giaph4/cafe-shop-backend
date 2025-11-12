package com.giapho.coffee_shop_backend.dto.shift;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;

public record ShiftTemplateRequestDTO(
        Long id,
        @NotBlank(message = "Tên ca không được để trống")
        @Size(max = 120, message = "Tên ca tối đa 120 ký tự")
        String name,
        @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
        String description,
        @NotNull(message = "Giờ bắt đầu không được để trống")
        LocalTime startTime,
        @NotNull(message = "Giờ kết thúc không được để trống")
        LocalTime endTime,
        Set<String> requiredRoles,
        BigDecimal defaultHourlyRate,
        BigDecimal defaultFixedAllowance
) {
}
