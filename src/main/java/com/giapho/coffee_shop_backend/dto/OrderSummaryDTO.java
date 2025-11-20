package com.giapho.coffee_shop_backend.dto;

import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.enums.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryDTO(
        Long id,
        String tableName,
        String staffUsername,
        OrderType type,
        OrderStatus status,
        BigDecimal subTotal,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime paidAt
) {
}
