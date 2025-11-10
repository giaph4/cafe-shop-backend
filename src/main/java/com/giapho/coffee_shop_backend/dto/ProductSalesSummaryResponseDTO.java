package com.giapho.coffee_shop_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSalesSummaryResponseDTO {
    private List<ProductSalesSummaryDTO> products;
    private Long totalQuantitySold;
    private BigDecimal totalRevenueGenerated;
}
