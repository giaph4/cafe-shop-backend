package com.giapho.coffee_shop_backend.domain.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CustomerPurchaseAggregate {
    Long getTotalOrders();
    BigDecimal getTotalAmount();
    LocalDateTime getLastPurchaseDate();
}
