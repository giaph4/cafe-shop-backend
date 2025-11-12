package com.giapho.coffee_shop_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAnalyticsDTO {
    private Long customerId;
    private String customerName;
    private String phone;
    private Long totalOrders;
    private BigDecimal totalSpent;
    private BigDecimal averageOrderValue;
    private Integer loyaltyPoints;
    private String lastOrderDate;

    public String getFullName() {
        return customerName;
    }

    public BigDecimal getAverageSpent() {
        return averageOrderValue;
    }
}
