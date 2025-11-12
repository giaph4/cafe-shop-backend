package com.giapho.coffee_shop_backend.dto.shift;

import com.giapho.coffee_shop_backend.domain.enums.AttendanceSource;

import java.time.LocalDateTime;

public record AttendanceRecordResponseDTO(
        Long id,
        Long assignmentId,
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt,
        AttendanceSource source,
        Integer lateMinutes,
        Integer earlyLeaveMinutes,
        String note,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
