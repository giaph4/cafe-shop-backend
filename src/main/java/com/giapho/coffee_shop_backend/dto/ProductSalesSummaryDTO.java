package com.giapho.coffee_shop_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSalesSummaryDTO {
    private Long productId;
    private String productName;
    private Long totalQuantitySold;
    private BigDecimal totalRevenueGenerated;
}
