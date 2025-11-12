package com.giapho.coffee_shop_backend.dto.shift;

import com.giapho.coffee_shop_backend.domain.enums.PayrollCycleStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Value
@Builder
public class PayrollCycleResponseDTO {
    Long id;
    String code;
    String name;
    LocalDate startDate;
    LocalDate endDate;
    PayrollCycleStatus status;
    String notes;
    String approvedBy;
    LocalDateTime approvedAt;
    String createdBy;
    String updatedBy;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
