package com.giapho.coffee_shop_backend.dto.shift;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@Builder
public class PayrollSummaryDTO {
    Long cycleId;
    String cycleCode;
    String cycleName;
    LocalDate cycleStartDate;
    LocalDate cycleEndDate;

    Long userId;
    String username;
    String fullName;

    Integer assignmentCount;
    Integer attendanceCount;
    Integer totalActualMinutes;
    Integer totalOrders;
    BigDecimal totalRevenue;
    BigDecimal totalBasePayroll;
    BigDecimal totalBonus;
    BigDecimal totalPenalty;
    BigDecimal totalAdjustment;
    BigDecimal totalNetPayroll;
    String notes;
}
