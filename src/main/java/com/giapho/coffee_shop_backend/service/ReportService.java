package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Expense;
import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.repository.*;
import com.giapho.coffee_shop_backend.dto.*;
import com.giapho.coffee_shop_backend.mapper.IngredientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.OrderDetail;

import java.util.*;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientMapper ingredientMapper;
    private final OrderDetailRepository orderDetailRepository;
    private final ExpenseRepository expenseRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public BigDecimal getDailyRevenue(LocalDate date) {
        return orderRepository.findTotalRevenueByDate(date);

    }

    @Transactional(readOnly = true)
    public List<IngredientResponseDTO> getCurrentInventory() {
        List<Ingredient> ingredients = ingredientRepository.findAll();
        return ingredients.stream()
                .map(ingredientMapper::entityToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IngredientResponseDTO> getLowStockIngredients() {
        List<Ingredient> lowStockIngredients = ingredientRepository.findIngredientsBelowReorderLevel();
        return lowStockIngredients.stream()
                .map(ingredientMapper::entityToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream exportOrdersToExcel(LocalDate startDate, LocalDate endDate) throws IOException {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        List<Order> orders = orderRepository.findByCreatedAtBetween(startDateTime, endDateTime, Pageable.unpaged()).getContent();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Orders");

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

            int rowIdx = 1;
            CellStyle dateCellStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));

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
                    if (itemsStr.length() > 2) {
                        itemsStr.setLength(itemsStr.length() - 2);
                    }
                }
                row.createCell(11).setCellValue(itemsStr.toString());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getProfitReport(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        BigDecimal totalRevenue = orderRepository.sumAmountBetweenDates(startDateTime, endDateTime);

        List<OrderDetail> paidDetails = orderDetailRepository.findPaidOrderDetailsBetweenDates(startDateTime, endDateTime);
        BigDecimal totalCostOfGoodsSold = BigDecimal.ZERO;

        for (OrderDetail detail : paidDetails) {
            BigDecimal productCost = detail.getProduct().getCost();
            if (productCost != null) {
                BigDecimal quantity = BigDecimal.valueOf(detail.getQuantity());
                totalCostOfGoodsSold = totalCostOfGoodsSold.add(productCost.multiply(quantity));
            } else {
                System.out.println("WARN: Product ID " + detail.getProduct().getId() + " has null cost.");
            }
        }


        BigDecimal totalProfit = totalRevenue.subtract(totalCostOfGoodsSold);

        return Map.of(
                "totalRevenue", totalRevenue,
                "totalCostOfGoodsSold", totalCostOfGoodsSold,
                "totalProfit", totalProfit
        );
    }

    @Transactional(readOnly = true)
    public List<BestSellerDTO> getBestSellingProducts(LocalDate startDate, LocalDate endDate, int top, String sortBy) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        Pageable topPageable = PageRequest.of(0, top);

        if ("revenue".equalsIgnoreCase(sortBy)) {
            return orderDetailRepository.findBestSellersByRevenueBetweenDates(startDateTime, endDateTime, topPageable);
        } else {
            return orderDetailRepository.findBestSellersByQuantityBetweenDates(startDateTime, endDateTime, topPageable);
        }
    }

    @Transactional(readOnly = true)
    public ProductSalesSummaryResponseDTO getProductSalesSummary(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<ProductSalesSummaryDTO> summaries = orderDetailRepository.findProductSalesSummaryBetweenDates(startDateTime, endDateTime);

        long totalQuantity = summaries.stream()
                .map(ProductSalesSummaryDTO::getTotalQuantitySold)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();

        BigDecimal totalRevenue = summaries.stream()
                .map(ProductSalesSummaryDTO::getTotalRevenueGenerated)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ProductSalesSummaryResponseDTO.builder()
                .products(summaries)
                .totalQuantitySold(totalQuantity)
                .totalRevenueGenerated(totalRevenue)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<LocalDate, BigDecimal> getRevenueReportByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<Order> paidOrders = orderRepository.findByStatusAndPaidAtBetween("PAID", startDateTime, endDateTime);

        Map<LocalDate, BigDecimal> dailyRevenue = paidOrders.stream()
                .filter(order -> order.getPaidAt() != null && order.getTotalAmount() != null)
                .collect(Collectors.groupingBy(
                        order -> order.getPaidAt().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Order::getTotalAmount,
                                BigDecimal::add
                        )
                ));

        fillMissingDates(dailyRevenue, startDate, endDate, BigDecimal.ZERO);

        return dailyRevenue;
    }

    @Transactional(readOnly = true)
    public Map<LocalDate, Map<String, BigDecimal>> getExpenseReportByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = expenseRepository.findByExpenseDateBetween(startDate, endDate, Pageable.unpaged()).getContent();

        Map<LocalDate, Map<String, BigDecimal>> dailyExpensesByCategory = expenses.stream()
                .filter(expense -> expense.getExpenseDate() != null && expense.getCategory() != null && expense.getAmount() != null)
                .collect(Collectors.groupingBy(
                        Expense::getExpenseDate,
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                Expense::getCategory,
                                LinkedHashMap::new,
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        Expense::getAmount,
                                        BigDecimal::add
                                )
                        )
                ));

        fillMissingDates(dailyExpensesByCategory, startDate, endDate, new LinkedHashMap<>());


        return dailyExpensesByCategory;
    }

    private <T> void fillMissingDates(Map<LocalDate, T> reportMap, LocalDate startDate, LocalDate endDate, T defaultValue) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        for (long i = 0; i <= daysBetween; i++) {
            LocalDate date = startDate.plusDays(i);
            reportMap.putIfAbsent(date, defaultValue);
        }
    }


    @Transactional(readOnly = true)
    public List<CustomerAnalyticsDTO> getTopCustomers(LocalDate startDate, LocalDate endDate, int top) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        Pageable topPageable = PageRequest.of(0, top);
        return customerRepository.findTopCustomersBetweenDates(startDateTime, endDateTime, topPageable);
    }

    @Transactional(readOnly = true)
    public List<StaffPerformanceDTO> getStaffPerformance(LocalDate startDate, LocalDate endDate, int top) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        Pageable topPageable = PageRequest.of(0, top);
        return userRepository.findStaffPerformanceBetweenDates(startDateTime, endDateTime, topPageable);
    }

    @Transactional(readOnly = true)
    public List<CategorySalesDTO> getCategorySales(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        List<CategorySalesDTO> categorySales = orderDetailRepository.findCategorySalesBetweenDates(startDateTime, endDateTime);

        BigDecimal totalRevenue = categorySales.stream()
                .map(CategorySalesDTO::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            categorySales.forEach(dto -> {
                double percentage = dto.getTotalRevenue()
                        .divide(totalRevenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
                dto.setRevenuePercentage(percentage);
            });
        }

        return categorySales;
    }

    @Transactional(readOnly = true)
    public List<HourlySalesDTO> getHourlySales(LocalDate date) {
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.plusDays(1).atStartOfDay();

        List<Order> orders = orderRepository.findByStatusAndPaidAtBetween("PAID", startDateTime, endDateTime);

        Map<Integer, List<Order>> ordersByHour = orders.stream()
                .filter(order -> order.getPaidAt() != null)
                .collect(Collectors.groupingBy(order -> order.getPaidAt().getHour()));

        List<HourlySalesDTO> hourlySales = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            List<Order> hourOrders = ordersByHour.getOrDefault(hour, Collections.emptyList());
            long orderCount = hourOrders.size();
            BigDecimal revenue = hourOrders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            double avgOrderValue = orderCount > 0 ? revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP).doubleValue() : 0.0;

            hourlySales.add(HourlySalesDTO.builder()
                    .hour(hour)
                    .orderCount(orderCount)
                    .revenue(revenue)
                    .averageOrderValue(avgOrderValue)
                    .build());
        }

        return hourlySales;
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodStatsDTO> getPaymentMethodStats(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<Order> orders = orderRepository.findByStatusAndPaidAtBetween("PAID", startDateTime, endDateTime);

        Map<String, List<Order>> ordersByPaymentMethod = orders.stream()
                .filter(order -> order.getPaymentMethod() != null)
                .collect(Collectors.groupingBy(Order::getPaymentMethod));

        BigDecimal totalAmount = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PaymentMethodStatsDTO> stats = new ArrayList<>();
        ordersByPaymentMethod.forEach((method, methodOrders) -> {
            long orderCount = methodOrders.size();
            BigDecimal methodTotal = methodOrders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            double percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
                    ? methodTotal.divide(totalAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;

            stats.add(PaymentMethodStatsDTO.builder()
                    .paymentMethod(method)
                    .orderCount(orderCount)
                    .totalAmount(methodTotal)
                    .percentage(percentage)
                    .build());
        });

        stats.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));
        return stats;
    }

    @Transactional(readOnly = true)
    public SalesComparisonDTO compareSalesPeriods(LocalDate currentStart, LocalDate currentEnd,
                                                   LocalDate previousStart, LocalDate previousEnd) {
        LocalDateTime currentStartDT = currentStart.atStartOfDay();
        LocalDateTime currentEndDT = currentEnd.plusDays(1).atStartOfDay();
        LocalDateTime previousStartDT = previousStart.atStartOfDay();
        LocalDateTime previousEndDT = previousEnd.plusDays(1).atStartOfDay();

        BigDecimal currentRevenue = orderRepository.sumAmountBetweenDates(currentStartDT, currentEndDT);
        BigDecimal previousRevenue = orderRepository.sumAmountBetweenDates(previousStartDT, previousEndDT);

        long currentOrders = orderRepository.findByStatusAndPaidAtBetween("PAID", currentStartDT, currentEndDT).size();
        long previousOrders = orderRepository.findByStatusAndPaidAtBetween("PAID", previousStartDT, previousEndDT).size();

        BigDecimal growthAmount = currentRevenue.subtract(previousRevenue);
        double growthPercentage = previousRevenue.compareTo(BigDecimal.ZERO) > 0
                ? growthAmount.divide(previousRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        return SalesComparisonDTO.builder()
                .period(currentStart + " to " + currentEnd)
                .currentRevenue(currentRevenue)
                .previousRevenue(previousRevenue)
                .growthAmount(growthAmount)
                .growthPercentage(growthPercentage)
                .currentOrders(currentOrders)
                .previousOrders(previousOrders)
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        BigDecimal todayRevenue = getDailyRevenue(today);
        BigDecimal monthRevenue = orderRepository.sumMonthRevenue();
        BigDecimal yearRevenue = orderRepository.sumYearRevenue();

        Long todayOrders = orderRepository.countTodayOrders();
        Long monthOrders = orderRepository.countMonthOrders();
        Long yearOrders = orderRepository.countYearOrders();

        Long totalCustomers = customerRepository.count();
        Long totalProducts = productRepository.count();
        Integer lowStockItems = ingredientRepository.findIngredientsBelowReorderLevel().size();

        BigDecimal avgOrderValue = todayOrders > 0
                ? todayRevenue.divide(BigDecimal.valueOf(todayOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, BigDecimal> todayProfit = getProfitReport(today, today);
        Map<String, BigDecimal> monthProfit = getProfitReport(firstDayOfMonth, today);

        return DashboardStatsDTO.builder()
                .todayRevenue(todayRevenue)
                .monthRevenue(monthRevenue)
                .yearRevenue(yearRevenue)
                .todayOrders(todayOrders)
                .monthOrders(monthOrders)
                .yearOrders(yearOrders)
                .totalCustomers(totalCustomers)
                .totalProducts(totalProducts)
                .lowStockItems(lowStockItems)
                .averageOrderValue(avgOrderValue)
                .todayProfit(todayProfit.get("totalProfit"))
                .monthProfit(monthProfit.get("totalProfit"))
                .build();
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream exportInventoryToExcel() throws IOException {
        List<Ingredient> ingredients = ingredientRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inventory");

            String[] headers = {"ID", "Name", "Unit", "Quantity", "Reorder Level", "Unit Price", "Total Value", "Status"};
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

            int rowIdx = 1;
            for (Ingredient ingredient : ingredients) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(ingredient.getId());
                row.createCell(1).setCellValue(ingredient.getName());
                row.createCell(2).setCellValue(ingredient.getUnit());
                row.createCell(3).setCellValue(ingredient.getQuantityOnHand() != null ? ingredient.getQuantityOnHand().doubleValue() : 0.0);
                row.createCell(4).setCellValue(ingredient.getReorderLevel() != null ? ingredient.getReorderLevel().doubleValue() : 0.0);
                row.createCell(5).setCellValue(0.0);

                row.createCell(6).setCellValue(0.0);

                String status = ingredient.getQuantityOnHand() != null && ingredient.getReorderLevel() != null
                        && ingredient.getQuantityOnHand().compareTo(ingredient.getReorderLevel()) <= 0
                        ? "Low Stock" : "In Stock";
                row.createCell(7).setCellValue(status);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream exportExpensesToExcel(LocalDate startDate, LocalDate endDate) throws IOException {
        List<Expense> expenses = expenseRepository.findByExpenseDateBetween(startDate, endDate, Pageable.unpaged()).getContent();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Expenses");

            String[] headers = {"ID", "Date", "Category", "Description", "Amount", "User"};
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

            int rowIdx = 1;
            for (Expense expense : expenses) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(expense.getId());
                row.createCell(1).setCellValue(expense.getExpenseDate() != null ? expense.getExpenseDate().toString() : "");
                row.createCell(2).setCellValue(expense.getCategory() != null ? expense.getCategory() : "");
                row.createCell(3).setCellValue(expense.getDescription() != null ? expense.getDescription() : "");
                row.createCell(4).setCellValue(expense.getAmount() != null ? expense.getAmount().doubleValue() : 0.0);
                row.createCell(5).setCellValue(expense.getUser() != null ? expense.getUser().getUsername() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalExpenses(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        BigDecimal total = expenseRepository.sumAmountByOptionalDateRange(startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalImportedIngredientCost(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(java.time.LocalTime.MAX) : null;

        BigDecimal total = purchaseOrderRepository.sumTotalAmountByStatusAndOptionalDateRange(
                "COMPLETED",
                startDateTime,
                endDateTime
        );

        return total != null ? total : BigDecimal.ZERO;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be greater than or equal to startDate");
        }
    }
}
