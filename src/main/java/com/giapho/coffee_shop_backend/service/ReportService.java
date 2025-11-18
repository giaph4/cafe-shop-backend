package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.BestSellerDTO;
import com.giapho.coffee_shop_backend.dto.CategorySalesDTO;
import com.giapho.coffee_shop_backend.dto.CustomerAnalyticsDTO;
import com.giapho.coffee_shop_backend.dto.DashboardStatsDTO;
import com.giapho.coffee_shop_backend.dto.HourlySalesDTO;
import com.giapho.coffee_shop_backend.dto.IngredientResponseDTO;
import com.giapho.coffee_shop_backend.dto.PaymentMethodStatsDTO;
import com.giapho.coffee_shop_backend.dto.ProductSalesSummaryResponseDTO;
import com.giapho.coffee_shop_backend.dto.SalesComparisonDTO;
import com.giapho.coffee_shop_backend.dto.StaffPerformanceDTO;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReportService {

    BigDecimal getDailyRevenue(LocalDate date);

    List<IngredientResponseDTO> getCurrentInventory();

    List<IngredientResponseDTO> getLowStockIngredients();

    ByteArrayInputStream exportOrdersToExcel(LocalDate startDate, LocalDate endDate);

    Map<String, BigDecimal> getProfitReport(LocalDate startDate, LocalDate endDate);

    List<BestSellerDTO> getBestSellingProducts(LocalDate startDate, LocalDate endDate, int top, String sortBy);

    ProductSalesSummaryResponseDTO getProductSalesSummary(LocalDate startDate, LocalDate endDate);

    Map<LocalDate, BigDecimal> getRevenueReportByDateRange(LocalDate startDate, LocalDate endDate);

    Map<LocalDate, Map<String, BigDecimal>> getExpenseReportByDateRange(LocalDate startDate, LocalDate endDate);

    List<CustomerAnalyticsDTO> getTopCustomers(LocalDate startDate, LocalDate endDate, int top);

    List<StaffPerformanceDTO> getStaffPerformance(LocalDate startDate, LocalDate endDate, int top);

    List<CategorySalesDTO> getCategorySales(LocalDate startDate, LocalDate endDate);

    List<HourlySalesDTO> getHourlySales(LocalDate date);

    List<PaymentMethodStatsDTO> getPaymentMethodStats(LocalDate startDate, LocalDate endDate);

    SalesComparisonDTO compareSalesPeriods(LocalDate currentStart, LocalDate currentEnd,
                                           LocalDate previousStart, LocalDate previousEnd);

    DashboardStatsDTO getDashboardStats();

    ByteArrayInputStream exportInventoryToExcel();

    ByteArrayInputStream exportExpensesToExcel(LocalDate startDate, LocalDate endDate);

    BigDecimal getTotalExpenses(LocalDate startDate, LocalDate endDate);

    BigDecimal getTotalImportedIngredientCost(LocalDate startDate, LocalDate endDate);
}
