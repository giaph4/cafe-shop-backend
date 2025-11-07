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
public class HourlySalesDTO {
    private Integer hour;
    private Long orderCount;
    private BigDecimal revenue;
    private Double averageOrderValue;
}
