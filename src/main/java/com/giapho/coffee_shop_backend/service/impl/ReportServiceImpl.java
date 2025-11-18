package com.giapho.coffee_shop_backend.service.impl;

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
import com.giapho.coffee_shop_backend.service.ReportService;
import com.giapho.coffee_shop_backend.service.report.core.ReportAggregationService;
import com.giapho.coffee_shop_backend.service.report.export.ReportExcelExportService;
import com.giapho.coffee_shop_backend.service.report.helper.ReportDateValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ReportAggregationService aggregationService;
    private final ReportExcelExportService excelExportService;

    @Override
    public BigDecimal getDailyRevenue(LocalDate date) {
        return aggregationService.getDailyRevenue(date);
    }

    @Override
    public List<IngredientResponseDTO> getCurrentInventory() {
        return aggregationService.getCurrentInventory();
    }

    @Override
    public List<IngredientResponseDTO> getLowStockIngredients() {
        return aggregationService.getLowStockIngredients();
    }

    @Override
    public ByteArrayInputStream exportOrdersToExcel(LocalDate startDate, LocalDate endDate) {
        ReportDateValidator.validateMandatoryRange(startDate, endDate);
        return excelExportService.exportOrders(startDate, endDate);
    }

    @Override
    public Map<String, BigDecimal> getProfitReport(LocalDate startDate, LocalDate endDate) {
        return aggregationService.getProfitReport(startDate, endDate);
    }

    @Override
    public List<BestSellerDTO> getBestSellingProducts(LocalDate startDate, LocalDate endDate, int top, String sortBy) {
        return aggregationService.getBestSellingProducts(startDate, endDate, top, sortBy);
    }

    @Override
    public ProductSalesSummaryResponseDTO getProductSalesSummary(LocalDate startDate, LocalDate endDate) {
        return aggregationService.getProductSalesSummary(startDate, endDate);
    }

    @Override
    public Map<LocalDate, BigDecimal> getRevenueReportByDateRange(LocalDate startDate, LocalDate endDate) {
        return aggregationService.getRevenueReportByDateRange(startDate, endDate);
    }

    @Override
    public Map<LocalDate, Map<String, BigDecimal>> getExpenseReportByDateRange(LocalDate startDate, LocalDate endDate) {
        return aggregationService.getExpenseReportByDateRange(startDate, endDate);
    }

    @Override
    public List<CustomerAnalyticsDTO> getTopCustomers(LocalDate startDate, LocalDate endDate, int top) {
        return aggregationService.getTopCustomers(startDate, endDate, top);
    }

    @Override
    public List<StaffPerformanceDTO> getStaffPerformance(LocalDate startDate, LocalDate endDate, int top) {
        return aggregationService.getStaffPerformance(startDate, endDate, top);
    }

    @Override
    public List<CategorySalesDTO> getCategorySales(LocalDate startDate, LocalDate endDate) {
        return aggregationService.getCategorySales(startDate, endDate);
    }

    @Override
    public List<HourlySalesDTO> getHourlySales(LocalDate date) {
        return aggregationService.getHourlySales(date);
    }

    @Override
    public List<PaymentMethodStatsDTO> getPaymentMethodStats(LocalDate startDate, LocalDate endDate) {
        return aggregationService.getPaymentMethodStats(startDate, endDate);
    }

    @Override
    public SalesComparisonDTO compareSalesPeriods(LocalDate currentStart, LocalDate currentEnd, LocalDate previousStart, LocalDate previousEnd) {
        return aggregationService.compareSalesPeriods(currentStart, currentEnd, previousStart, previousEnd);
    }

    @Override
    public DashboardStatsDTO getDashboardStats() {
        return aggregationService.getDashboardStats();
    }

    @Override
    public ByteArrayInputStream exportInventoryToExcel() {
        return excelExportService.exportInventory();
    }

    @Override
    public ByteArrayInputStream exportExpensesToExcel(LocalDate startDate, LocalDate endDate) {
        ReportDateValidator.validateMandatoryRange(startDate, endDate);
        return excelExportService.exportExpenses(startDate, endDate);
    }

    @Override
    public BigDecimal getTotalExpenses(LocalDate startDate, LocalDate endDate) {
        return aggregationService.getTotalExpenses(startDate, endDate);
    }

    @Override
    public BigDecimal getTotalImportedIngredientCost(LocalDate startDate, LocalDate endDate) {
        return aggregationService.getTotalImportedIngredientCost(startDate, endDate);
    }
}
