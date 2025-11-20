package com.giapho.coffee_shop_backend.dto.shift;

import com.giapho.coffee_shop_backend.domain.enums.ShiftSessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ShiftReportResponseDTO(
        Long reportId,
        Long sessionId,
        Long workShiftId,
        Long userId,
        String username,
        ShiftSessionStatus status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        int totalOrders,
        BigDecimal totalPaidAmount,
        BigDecimal totalUnpaidAmount,
        int transferredOrders,
        List<ShiftReportPaymentBreakdownDTO> paymentBreakdown,
        List<ShiftReportProductDTO> topProducts,
        LocalDateTime generatedAt
) {
}
