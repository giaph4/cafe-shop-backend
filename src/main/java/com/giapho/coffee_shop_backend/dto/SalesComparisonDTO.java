package com.giapho.coffee_shop_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesComparisonDTO {
    private String period;
    private BigDecimal currentRevenue;
    private BigDecimal previousRevenue;
    private BigDecimal growthAmount;
    private Double growthPercentage;
    private Long currentOrders;
    private Long previousOrders;
}
