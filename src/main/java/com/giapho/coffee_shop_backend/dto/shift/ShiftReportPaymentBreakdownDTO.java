package com.giapho.coffee_shop_backend.dto.shift;

import java.math.BigDecimal;

public record ShiftReportPaymentBreakdownDTO(
        String paymentMethod,
        int orderCount,
        BigDecimal totalAmount
) {
}
