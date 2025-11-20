package com.giapho.coffee_shop_backend.domain.repository.projection;

import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.enums.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderSummaryProjection {

    Long getId();

    String getTableName();

    String getStaffUsername();

    OrderType getType();

    OrderStatus getStatus();

    BigDecimal getSubTotal();

    BigDecimal getTotalAmount();

    LocalDateTime getCreatedAt();

    LocalDateTime getPaidAt();
}
