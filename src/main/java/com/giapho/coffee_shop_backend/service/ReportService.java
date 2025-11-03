package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Expense;
import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.repository.ExpenseRepository;
import com.giapho.coffee_shop_backend.domain.repository.IngredientRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderDetailRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.dto.BestSellerDTO;
import com.giapho.coffee_shop_backend.dto.IngredientResponseDTO;
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

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.temporal.ChronoUnit;
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
    private final IngredientMapper ingredientMapper;
    private final OrderDetailRepository orderDetailRepository;
    private final ExpenseRepository expenseRepository;

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
}