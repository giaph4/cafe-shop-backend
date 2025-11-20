package com.giapho.coffee_shop_backend.dto.shift;

import java.math.BigDecimal;

public record ShiftReportProductDTO(
        Long productId,
        String productName,
        int quantity,
        BigDecimal totalAmount
) {
}
