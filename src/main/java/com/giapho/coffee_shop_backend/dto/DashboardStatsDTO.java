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
public class DashboardStatsDTO {
    private BigDecimal todayRevenue;
    private BigDecimal monthRevenue;
    private BigDecimal yearRevenue;
    private Long todayOrders;
    private Long monthOrders;
    private Long yearOrders;
    private Long totalCustomers;
    private Long totalProducts;
    private Integer lowStockItems;
    private BigDecimal averageOrderValue;
    private BigDecimal todayProfit;
    private BigDecimal monthProfit;
}
