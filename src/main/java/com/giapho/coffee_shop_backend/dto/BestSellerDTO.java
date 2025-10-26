package com.giapho.coffee_shop_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor // Cần constructor này cho JPQL query
public class BestSellerDTO {
    private Long productId;
    private String productName;
    private Long totalQuantitySold; // Tổng số lượng bán được
    private BigDecimal totalRevenueGenerated; // Tổng doanh thu tạo ra
}