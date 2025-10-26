package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Expense;
import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.repository.ExpenseRepository;
import com.giapho.coffee_shop_backend.domain.repository.IngredientRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderDetailRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.dto.BestSellerDTO;
import com.giapho.coffee_shop_backend.dto.IngredientResponseDTO; // Dùng lại DTO cũ
import com.giapho.coffee_shop_backend.mapper.IngredientMapper; // Dùng lại mapper cũ
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.usermodel.*; // Import tất cả từ usermodel
import org.apache.poi.xssf.usermodel.XSSFWorkbook; // Cụ thể cho .xlsx

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.giapho.coffee_shop_backend.domain.entity.Order; // Import Order entity
import com.giapho.coffee_shop_backend.domain.entity.OrderDetail; // Import OrderDetail

import java.util.List; // Import List
import java.util.LinkedHashMap; // Import LinkedHashMap
import java.util.Map;
import java.time.temporal.ChronoUnit; // Import ChronoUnit
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientMapper ingredientMapper; // Inject mapper
    private final OrderDetailRepository orderDetailRepository;
    private final ExpenseRepository expenseRepository;

    /**
     * Lấy tổng doanh thu trong một ngày cụ thể
     */
    @Transactional(readOnly = true)
    public BigDecimal getDailyRevenue(LocalDate date) {
        // Cách 1: Dùng hàm DATE()
        return orderRepository.findTotalRevenueByDate(date);

        // Cách 2: Dùng khoảng thời gian
        // LocalDateTime startOfDay = date.atStartOfDay(); // Ví dụ: 2025-10-26T00:00:00
        // LocalDateTime endOfDay = date.atTime(LocalTime.MAX); // Ví dụ: 2025-10-26T23:59:59.999...
        // return orderRepository.findTotalRevenueBetween(startOfDay, endOfDay.plusNanos(1)); // Dùng < endOfDay + 1 nano
    }

    /**
     * Lấy danh sách tồn kho hiện tại (map sang DTO)
     */
    @Transactional(readOnly = true)
    public List<IngredientResponseDTO> getCurrentInventory() {
        List<Ingredient> ingredients = ingredientRepository.findAll();
        // Dùng Stream API và mapper để chuyển đổi
        return ingredients.stream()
                .map(ingredientMapper::entityToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách các nguyên liệu sắp hết hàng
     */
    @Transactional(readOnly = true)
    public List<IngredientResponseDTO> getLowStockIngredients() {
        List<Ingredient> lowStockIngredients = ingredientRepository.findIngredientsBelowReorderLevel();
        return lowStockIngredients.stream()
                .map(ingredientMapper::entityToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Xuất danh sách Order ra file Excel (ByteArrayInputStream)
     *
     * @param startDate Ngày bắt đầu
     * @param endDate   Ngày kết thúc
     * @return ByteArrayInputStream chứa dữ liệu file Excel
     * @throws IOException Lỗi khi tạo file
     */
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportOrdersToExcel(LocalDate startDate, LocalDate endDate) throws IOException {
        // Lấy danh sách Order trong khoảng thời gian (không phân trang cho export)
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay(); // Lấy đến hết ngày kết thúc
        List<Order> orders = orderRepository.findByCreatedAtBetween(startDateTime, endDateTime, Pageable.unpaged()).getContent();

        // Tạo workbook Excel (.xlsx)
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Orders");

            // --- Tạo Header Row ---
            String[] headers = {"Order ID", "Table", "Staff", "Type", "Status", "Created At", "Paid At", "Payment Method", "SubTotal", "Discount", "Total Amount", "Items"};
            Row headerRow = sheet.createRow(0);
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerCellStyle.setFont(headerFont);

            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerCellStyle);
            }

            // --- Tạo Data Rows ---
            int rowIdx = 1;
            CellStyle dateCellStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss")); // Format ngày giờ

            for (Order order : orders) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(order.getId() != null ? order.getId() : 0);
                row.createCell(1).setCellValue(order.getCafeTable() != null ? order.getCafeTable().getName() : "Take Away/Delivery");
                row.createCell(2).setCellValue(order.getUser() != null ? order.getUser().getUsername() : "N/A");
                row.createCell(3).setCellValue(order.getType() != null ? order.getType() : "");
                row.createCell(4).setCellValue(order.getStatus() != null ? order.getStatus() : "");

                Cell createdAtCell = row.createCell(5);
                if (order.getCreatedAt() != null) {
                    createdAtCell.setCellValue(order.getCreatedAt());
                    createdAtCell.setCellStyle(dateCellStyle);
                }

                Cell paidAtCell = row.createCell(6);
                if (order.getPaidAt() != null) {
                    paidAtCell.setCellValue(order.getPaidAt());
                    paidAtCell.setCellStyle(dateCellStyle);
                }

                row.createCell(7).setCellValue(order.getPaymentMethod() != null ? order.getPaymentMethod() : "");
                row.createCell(8).setCellValue(order.getSubTotal() != null ? order.getSubTotal().doubleValue() : 0.0);
                row.createCell(9).setCellValue(order.getDiscountAmount() != null ? order.getDiscountAmount().doubleValue() : 0.0);
                row.createCell(10).setCellValue(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0);

                // Ghép chuỗi thông tin items
                StringBuilder itemsStr = new StringBuilder();
                if (order.getOrderDetails() != null) {
                    for (OrderDetail detail : order.getOrderDetails()) {
                        if (detail.getProduct() != null) {
                            itemsStr.append(detail.getProduct().getName())
                                    .append(" (x")
                                    .append(detail.getQuantity())
                                    .append("), ");
                        }
                    }
                    // Xóa dấu phẩy cuối
                    if (itemsStr.length() > 2) {
                        itemsStr.setLength(itemsStr.length() - 2);
                    }
                }
                row.createCell(11).setCellValue(itemsStr.toString());
            }

            // Tự động điều chỉnh độ rộng cột
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Ghi workbook vào ByteArrayOutputStream
            workbook.write(out);
            // Tạo ByteArrayInputStream từ output stream
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    /**
     * Tính toán Lợi nhuận trong khoảng thời gian
     * Lợi nhuận = Tổng Doanh thu (PAID Orders) - Tổng Giá vốn Hàng bán (COGS)
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getProfitReport(LocalDate startDate, LocalDate endDate) {
        // ---- SỬA CÁCH TÍNH startDateTime và endDateTime ----
        LocalDateTime startDateTime = startDate.atStartOfDay(); // Bắt đầu ngày startDate (00:00:00)
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay(); // Bắt đầu ngày KẾ TIẾP của endDate (00:00:00)
        // --------------------------------------------------

        // 1. Tính Tổng Doanh thu (Sử dụng phương thức mới trong Repository)
        BigDecimal totalRevenue = orderRepository.sumAmountBetweenDates(startDateTime, endDateTime); // Gọi hàm đúng

        // 2. Tính Tổng Giá vốn Hàng bán (COGS)
        // Dùng startDateTime và endDateTime đã tính
        List<OrderDetail> paidDetails = orderDetailRepository.findPaidOrderDetailsBetweenDates(startDateTime, endDateTime);
        BigDecimal totalCostOfGoodsSold = BigDecimal.ZERO;

        // ... (phần tính COGS giữ nguyên) ...
        for (OrderDetail detail : paidDetails) {
            BigDecimal productCost = detail.getProduct().getCost();
            if (productCost != null) {
                BigDecimal quantity = BigDecimal.valueOf(detail.getQuantity());
                totalCostOfGoodsSold = totalCostOfGoodsSold.add(productCost.multiply(quantity));
            } else {
                System.out.println("WARN: Product ID " + detail.getProduct().getId() + " has null cost.");
            }
        }


        // 3. Tính Lợi nhuận
        BigDecimal totalProfit = totalRevenue.subtract(totalCostOfGoodsSold);

        // 4. Trả về kết quả
        return Map.of(
                "totalRevenue", totalRevenue,
                "totalCostOfGoodsSold", totalCostOfGoodsSold,
                "totalProfit", totalProfit
        );
    }

    /**
     * Lấy danh sách sản phẩm bán chạy nhất trong khoảng thời gian.
     *
     * @param startDate Ngày bắt đầu
     * @param endDate   Ngày kết thúc
     * @param top       N Số lượng sản phẩm top cần lấy
     * @param sortBy    'quantity' hoặc 'revenue'
     * @return Danh sách BestSellerDTO
     */
    @Transactional(readOnly = true)
    public List<BestSellerDTO> getBestSellingProducts(LocalDate startDate, LocalDate endDate, int top, String sortBy) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // Tạo Pageable để giới hạn số lượng kết quả (chỉ lấy trang đầu tiên)
        Pageable topPageable = PageRequest.of(0, top);

        if ("revenue".equalsIgnoreCase(sortBy)) {
            return orderDetailRepository.findBestSellersByRevenueBetweenDates(startDateTime, endDateTime, topPageable);
        } else {
            // Mặc định sắp xếp theo số lượng
            return orderDetailRepository.findBestSellersByQuantityBetweenDates(startDateTime, endDateTime, topPageable);
        }
    }

    /**
     * Lấy báo cáo Doanh thu theo từng ngày trong khoảng thời gian.
     * @return Map<LocalDate, BigDecimal> (Ngày -> Tổng Doanh thu ngày đó)
     */
    @Transactional(readOnly = true)
    public Map<LocalDate, BigDecimal> getRevenueReportByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        // Lấy đến *cuối* ngày endDate
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // Lấy tất cả các đơn hàng PAID trong khoảng thời gian
        List<Order> paidOrders = orderRepository.findByStatusAndPaidAtBetween("PAID", startDateTime, endDateTime);

        // Sử dụng Stream API để nhóm theo ngày và tính tổng
        Map<LocalDate, BigDecimal> dailyRevenue = paidOrders.stream()
                .filter(order -> order.getPaidAt() != null && order.getTotalAmount() != null) // Lọc bỏ null an toàn
                .collect(Collectors.groupingBy(
                        order -> order.getPaidAt().toLocalDate(), // Nhóm theo ngày (LocalDate)
                        LinkedHashMap::new, // Giữ thứ tự ngày
                        Collectors.reducing(
                                BigDecimal.ZERO,      // Giá trị khởi tạo
                                Order::getTotalAmount,  // Lấy totalAmount
                                BigDecimal::add       // Cộng dồn
                        )
                ));

        // (Tùy chọn) Điền các ngày không có doanh thu bằng 0
        fillMissingDates(dailyRevenue, startDate, endDate, BigDecimal.ZERO);

        return dailyRevenue;
    }

    /**
     * Lấy báo cáo Chi phí theo từng ngày và theo loại trong khoảng thời gian.
     * @return Map<LocalDate, Map<String, BigDecimal>> (Ngày -> (Loại chi phí -> Tổng tiền loại đó))
     */
    @Transactional(readOnly = true)
    public Map<LocalDate, Map<String, BigDecimal>> getExpenseReportByDateRange(LocalDate startDate, LocalDate endDate) {
        // Lấy tất cả chi phí trong khoảng thời gian
        List<Expense> expenses = expenseRepository.findByExpenseDateBetween(startDate, endDate, Pageable.unpaged()).getContent();

        // Nhóm theo ngày, sau đó nhóm tiếp theo loại chi phí và tính tổng
        Map<LocalDate, Map<String, BigDecimal>> dailyExpensesByCategory = expenses.stream()
                .filter(expense -> expense.getExpenseDate() != null && expense.getCategory() != null && expense.getAmount() != null)
                .collect(Collectors.groupingBy(
                        Expense::getExpenseDate, // Nhóm theo ngày chi
                        LinkedHashMap::new,      // Giữ thứ tự ngày
                        Collectors.groupingBy(
                                Expense::getCategory, // Nhóm tiếp theo loại chi phí
                                LinkedHashMap::new,   // Giữ thứ tự loại
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        Expense::getAmount,
                                        BigDecimal::add
                                )
                        )
                ));

        // (Tùy chọn) Điền các ngày không có chi phí bằng Map rỗng
        fillMissingDates(dailyExpensesByCategory, startDate, endDate, new LinkedHashMap<>());


        return dailyExpensesByCategory;
    }

    /**
     * (Hàm helper) Điền các ngày còn thiếu trong Map báo cáo với giá trị mặc định.
     */
    private <T> void fillMissingDates(Map<LocalDate, T> reportMap, LocalDate startDate, LocalDate endDate, T defaultValue) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        for (long i = 0; i <= daysBetween; i++) {
            LocalDate date = startDate.plusDays(i);
            reportMap.putIfAbsent(date, defaultValue); // Chỉ thêm nếu ngày đó chưa có
        }
    }
}