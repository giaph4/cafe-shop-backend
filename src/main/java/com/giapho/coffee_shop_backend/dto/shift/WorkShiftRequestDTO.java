package com.giapho.coffee_shop_backend.dto.shift;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record WorkShiftRequestDTO(
        @NotBlank(message = "Tên ca không được để trống")
        String name,
        String description,
        @NotNull(message = "Thời gian bắt đầu không được để trống")
        LocalDateTime startAt,
        @NotNull(message = "Thời gian kết thúc không được để trống")
        @Future(message = "Thời gian kết thúc phải ở tương lai")
        LocalDateTime endAt,
        @NotNull(message = "Số lượng nhân viên tối đa không được để trống")
        @Min(value = 1, message = "Ca phải có ít nhất 1 nhân viên")
        Integer maxEmployees,
        Boolean active
) {
}
