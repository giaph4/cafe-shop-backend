package com.giapho.coffee_shop_backend.service.report.core;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReportAggregationService {

    private final RevenueReportProvider revenueReportProvider;
    private final InventoryReportProvider inventoryReportProvider;
    private final ExpenseReportProvider expenseReportProvider;
    private final AnalyticsReportProvider analyticsReportProvider;

    public BigDecimal getDailyRevenue(LocalDate date) {
        return revenueReportProvider.getDailyRevenue(date);
    }

    public List<IngredientResponseDTO> getCurrentInventory() {
        return inventoryReportProvider.getCurrentInventory();
    }

    public List<IngredientResponseDTO> getLowStockIngredients() {
        return inventoryReportProvider.getLowStockIngredients();
    }

    public Map<String, BigDecimal> getProfitReport(LocalDate startDate, LocalDate endDate) {
        return revenueReportProvider.getProfitReport(startDate, endDate);
    }

    public List<BestSellerDTO> getBestSellingProducts(LocalDate startDate, LocalDate endDate, int top, String sortBy) {
        return revenueReportProvider.getBestSellingProducts(startDate, endDate, top, sortBy);
    }

    public ProductSalesSummaryResponseDTO getProductSalesSummary(LocalDate startDate, LocalDate endDate) {
        return revenueReportProvider.getProductSalesSummary(startDate, endDate);
    }

    public Map<LocalDate, BigDecimal> getRevenueReportByDateRange(LocalDate startDate, LocalDate endDate) {
        return revenueReportProvider.getRevenueReportByDateRange(startDate, endDate);
    }

    public Map<LocalDate, Map<String, BigDecimal>> getExpenseReportByDateRange(LocalDate startDate, LocalDate endDate) {
        return expenseReportProvider.getExpenseReportByDateRange(startDate, endDate);
    }

    public List<CustomerAnalyticsDTO> getTopCustomers(LocalDate startDate, LocalDate endDate, int top) {
        return analyticsReportProvider.getTopCustomers(startDate, endDate, top);
    }

    public List<StaffPerformanceDTO> getStaffPerformance(LocalDate startDate, LocalDate endDate, int top) {
        return analyticsReportProvider.getStaffPerformance(startDate, endDate, top);
    }

    public List<CategorySalesDTO> getCategorySales(LocalDate startDate, LocalDate endDate) {
        return revenueReportProvider.getCategorySales(startDate, endDate);
    }

    public List<HourlySalesDTO> getHourlySales(LocalDate date) {
        return revenueReportProvider.getHourlySales(date);
    }

    public List<PaymentMethodStatsDTO> getPaymentMethodStats(LocalDate startDate, LocalDate endDate) {
        return revenueReportProvider.getPaymentMethodStats(startDate, endDate);
    }

    public SalesComparisonDTO compareSalesPeriods(LocalDate currentStart, LocalDate currentEnd,
            LocalDate previousStart, LocalDate previousEnd) {
        return revenueReportProvider.compareSalesPeriods(currentStart, currentEnd, previousStart, previousEnd);
    }

    public DashboardStatsDTO getDashboardStats() {
        return analyticsReportProvider.getDashboardStats();
    }

    public BigDecimal getTotalExpenses(LocalDate startDate, LocalDate endDate) {
        return expenseReportProvider.getTotalExpenses(startDate, endDate);
    }

    public BigDecimal getTotalImportedIngredientCost(LocalDate startDate, LocalDate endDate) {
        return inventoryReportProvider.getTotalImportedIngredientCost(startDate, endDate);
    }
}
