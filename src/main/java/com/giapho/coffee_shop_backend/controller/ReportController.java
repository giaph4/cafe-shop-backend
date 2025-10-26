package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.BestSellerDTO;
import com.giapho.coffee_shop_backend.dto.IngredientResponseDTO;
import com.giapho.coffee_shop_backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map; // Import Map

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * API Lấy tổng doanh thu theo ngày
     * Chỉ MANAGER hoặc ADMIN mới có quyền xem.
     */
    @GetMapping("/daily-revenue")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getDailyRevenue(
            // @RequestParam để lấy tham số từ URL (?date=...)
            // @DateTimeFormat để Spring biết cách chuyển chuỗi "YYYY-MM-DD" thành LocalDate
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        BigDecimal revenue = reportService.getDailyRevenue(date);
        // Trả về JSON có cấu trúc rõ ràng
        Map<String, Object> response = Map.of(
                "date", date.toString(),
                "totalRevenue", revenue
        );
        return ResponseEntity.ok(response);
    }

    /**
     * API Lấy báo cáo tồn kho hiện tại
     * Chỉ MANAGER hoặc ADMIN mới có quyền xem.
     * Có thể thêm tham số ?lowStock=true để chỉ lấy hàng sắp hết
     */
    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<IngredientResponseDTO>> getInventoryReport(
            @RequestParam(required = false, defaultValue = "false") boolean lowStock // Tham số tùy chọn
    ) {
        List<IngredientResponseDTO> inventory;
        if (lowStock) {
            inventory = reportService.getLowStockIngredients();
        } else {
            inventory = reportService.getCurrentInventory();
        }
        return ResponseEntity.ok(inventory);
    }

    /**
     * API Xuất danh sách Order ra file Excel
     * Chỉ MANAGER hoặc ADMIN mới có quyền.
     */
    @GetMapping("/orders/export")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Resource> exportOrders(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            ByteArrayInputStream excelStream = reportService.exportOrdersToExcel(startDate, endDate);
            InputStreamResource resource = new InputStreamResource(excelStream);

            // Tạo tên file động
            String filename = "Orders_" + startDate + "_to_" + endDate + ".xlsx";

            return ResponseEntity.ok()
                    // Header cho trình duyệt biết đây là file đính kèm để tải xuống
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    // Header cho biết loại file là Excel
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(resource);

        } catch (IOException e) {
            // Có thể trả về lỗi 500 hoặc một response lỗi cụ thể hơn
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * API Lấy báo cáo Lợi nhuận theo khoảng ngày
     * Chỉ MANAGER hoặc ADMIN mới có quyền xem.
     */
    @GetMapping("/profit")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getProfitReport(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Map<String, BigDecimal> profitData = reportService.getProfitReport(startDate, endDate);

        // Tạo Map response để có cấu trúc rõ ràng, bao gồm cả ngày
        Map<String, Object> response = Map.of(
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "totalRevenue", profitData.getOrDefault("totalRevenue", BigDecimal.ZERO),
                "totalCostOfGoodsSold", profitData.getOrDefault("totalCostOfGoodsSold", BigDecimal.ZERO),
                "totalProfit", profitData.getOrDefault("totalProfit", BigDecimal.ZERO)
        );
        return ResponseEntity.ok(response);
    }

    /**
     * API Lấy danh sách sản phẩm bán chạy nhất
     * Chỉ MANAGER hoặc ADMIN mới có quyền xem.
     */
    @GetMapping("/best-sellers")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<BestSellerDTO>> getBestSellers(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int top, // Mặc định lấy top 10
            @RequestParam(defaultValue = "quantity") String sortBy // Mặc định theo số lượng ('quantity' hoặc 'revenue')
    ) {
        // Có thể thêm validation cho sortBy
        if (!sortBy.equalsIgnoreCase("quantity") && !sortBy.equalsIgnoreCase("revenue")) {
            sortBy = "quantity"; // Mặc định nếu giá trị không hợp lệ
        }
        List<BestSellerDTO> bestSellers = reportService.getBestSellingProducts(startDate, endDate, top, sortBy);
        return ResponseEntity.ok(bestSellers);
    }

    /**
     * API Lấy báo cáo Doanh thu chi tiết theo từng ngày trong khoảng thời gian
     * Chỉ MANAGER hoặc ADMIN mới có quyền xem.
     */
    @GetMapping("/revenue-by-date")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<LocalDate, BigDecimal>> getRevenueByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Map<LocalDate, BigDecimal> dailyRevenue = reportService.getRevenueReportByDateRange(startDate, endDate);
        return ResponseEntity.ok(dailyRevenue);
    }

    /**
     * API Lấy báo cáo Chi phí chi tiết theo từng ngày và loại trong khoảng thời gian
     * Chỉ MANAGER hoặc ADMIN mới có quyền xem.
     */
    @GetMapping("/expenses-by-date")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<LocalDate, Map<String, BigDecimal>>> getExpensesByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Map<LocalDate, Map<String, BigDecimal>> dailyExpenses = reportService.getExpenseReportByDateRange(startDate, endDate);
        return ResponseEntity.ok(dailyExpenses);
    }
}