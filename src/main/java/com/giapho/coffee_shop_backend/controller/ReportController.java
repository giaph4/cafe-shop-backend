package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.*;
import com.giapho.coffee_shop_backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;


    @GetMapping("/daily-revenue")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getDailyRevenue(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        BigDecimal revenue = reportService.getDailyRevenue(date);
        Map<String, Object> response = Map.of(
                "date", date.toString(),
                "totalRevenue", revenue
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<IngredientResponseDTO>> getInventoryReport(
            @RequestParam(required = false, defaultValue = "false") boolean lowStock
    ) {
        List<IngredientResponseDTO> inventory;
        if (lowStock) {
            inventory = reportService.getLowStockIngredients();
        } else {
            inventory = reportService.getCurrentInventory();
        }
        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/orders/export")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Resource> exportOrders(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        InputStreamResource resource = new InputStreamResource(reportService.exportOrdersToExcel(startDate, endDate));
        String filename = "Orders_" + startDate + "_to_" + endDate + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(resource);
    }

    @GetMapping("/profit")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getProfitReport(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Map<String, BigDecimal> profitData = reportService.getProfitReport(startDate, endDate);

        Map<String, Object> response = Map.of(
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "totalRevenue", profitData.getOrDefault("totalRevenue", BigDecimal.ZERO),
                "totalCostOfGoodsSold", profitData.getOrDefault("totalCostOfGoodsSold", BigDecimal.ZERO),
                "totalProfit", profitData.getOrDefault("totalProfit", BigDecimal.ZERO)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/best-sellers")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<BestSellerDTO>> getBestSellers(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int top,
            @RequestParam(defaultValue = "quantity") String sortBy
    ) {
        if (!sortBy.equalsIgnoreCase("quantity") && !sortBy.equalsIgnoreCase("revenue")) {
            sortBy = "quantity";
        }
        List<BestSellerDTO> bestSellers = reportService.getBestSellingProducts(startDate, endDate, top, sortBy);
        return ResponseEntity.ok(bestSellers);
    }

    @GetMapping("/product-sales-summary")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductSalesSummaryResponseDTO> getProductSalesSummary(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        ProductSalesSummaryResponseDTO summary = reportService.getProductSalesSummary(startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/revenue-by-date")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<LocalDate, BigDecimal>> getRevenueByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Map<LocalDate, BigDecimal> dailyRevenue = reportService.getRevenueReportByDateRange(startDate, endDate);
        return ResponseEntity.ok(dailyRevenue);
    }

    @GetMapping("/expenses-by-date")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<LocalDate, Map<String, BigDecimal>>> getExpensesByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Map<LocalDate, Map<String, BigDecimal>> dailyExpenses = reportService.getExpenseReportByDateRange(startDate, endDate);
        return ResponseEntity.ok(dailyExpenses);
    }

    @GetMapping("/top-customers")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<CustomerAnalyticsDTO>> getTopCustomers(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int top
    ) {
        List<CustomerAnalyticsDTO> topCustomers = reportService.getTopCustomers(startDate, endDate, top);
        return ResponseEntity.ok(topCustomers);
    }

    @GetMapping("/staff-performance")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<StaffPerformanceDTO>> getStaffPerformance(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int top
    ) {
        List<StaffPerformanceDTO> staffPerformance = reportService.getStaffPerformance(startDate, endDate, top);
        return ResponseEntity.ok(staffPerformance);
    }

    @GetMapping("/category-sales")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<CategorySalesDTO>> getCategorySales(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<CategorySalesDTO> categorySales = reportService.getCategorySales(startDate, endDate);
        return ResponseEntity.ok(categorySales);
    }

    @GetMapping("/hourly-sales")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<HourlySalesDTO>> getHourlySales(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<HourlySalesDTO> hourlySales = reportService.getHourlySales(date);
        return ResponseEntity.ok(hourlySales);
    }

    @GetMapping("/payment-method-stats")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<PaymentMethodStatsDTO>> getPaymentMethodStats(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<PaymentMethodStatsDTO> stats = reportService.getPaymentMethodStats(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/sales-comparison")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<SalesComparisonDTO> compareSales(
            @RequestParam("currentStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate currentStart,
            @RequestParam("currentEnd") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate currentEnd,
            @RequestParam("previousStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate previousStart,
            @RequestParam("previousEnd") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate previousEnd
    ) {
        SalesComparisonDTO comparison = reportService.compareSalesPeriods(currentStart, currentEnd, previousStart, previousEnd);
        return ResponseEntity.ok(comparison);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        DashboardStatsDTO stats = reportService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/inventory/export")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Resource> exportInventory() {
        InputStreamResource resource = new InputStreamResource(reportService.exportInventoryToExcel());
        String filename = "Inventory_" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(resource);
    }

    @GetMapping("/expenses/export")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Resource> exportExpenses(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        InputStreamResource resource = new InputStreamResource(reportService.exportExpensesToExcel(startDate, endDate));
        String filename = "Expenses_" + startDate + "_to_" + endDate + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(resource);
    }

    @GetMapping("/total-expenses")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getTotalExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        BigDecimal total = reportService.getTotalExpenses(startDate, endDate);
        return ResponseEntity.ok(Map.of(
                "startDate", startDate,
                "endDate", endDate,
                "totalExpenses", total
        ));
    }

    @GetMapping("/total-imported-ingredients")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getTotalImportedIngredientCost(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        BigDecimal total = reportService.getTotalImportedIngredientCost(startDate, endDate);
        return ResponseEntity.ok(Map.of(
                "startDate", startDate,
                "endDate", endDate,
                "totalImportedIngredientCost", total
        ));
    }
}