package com.giapho.coffee_shop_backend.service.report.export;

import com.giapho.coffee_shop_backend.domain.entity.Expense;
import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.OrderDetail;
import com.giapho.coffee_shop_backend.domain.repository.ExpenseRepository;
import com.giapho.coffee_shop_backend.domain.repository.IngredientRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.exception.report.ReportExportException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportExcelExportService {

    private final OrderRepository orderRepository;
    private final IngredientRepository ingredientRepository;
    private final ExpenseRepository expenseRepository;

    public ByteArrayInputStream exportOrders(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end, Pageable.unpaged()).getContent();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            List<String> headers = Arrays.asList(
                    "Order ID", "Table", "Staff", "Type", "Status", "Created At", "Paid At",
                    "Payment Method", "SubTotal", "Discount", "Total Amount", "Items");
            Sheet sheet = ExcelSheetBuilder.createSheetWithHeader(workbook, "Orders", headers);

            CellStyle dateTimeStyle = buildDateTimeCellStyle(workbook);
            int rowIdx = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(order.getId() != null ? order.getId() : 0);
                row.createCell(1).setCellValue(order.getCafeTable() != null ? order.getCafeTable().getName() : "Take Away/Delivery");
                row.createCell(2).setCellValue(order.getUser() != null ? order.getUser().getUsername() : "N/A");
                row.createCell(3).setCellValue(order.getType() != null ? order.getType().name() : "");
                row.createCell(4).setCellValue(order.getStatus() != null ? order.getStatus().name() : "");

                populateDateCell(row, 5, order.getCreatedAt(), dateTimeStyle);
                populateDateCell(row, 6, order.getPaidAt(), dateTimeStyle);

                row.createCell(7).setCellValue(order.getPaymentMethod() != null ? order.getPaymentMethod() : "");
                row.createCell(8).setCellValue(toDouble(order.getSubTotal()));
                row.createCell(9).setCellValue(toDouble(order.getDiscountAmount()));
                row.createCell(10).setCellValue(toDouble(order.getTotalAmount()));
                row.createCell(11).setCellValue(buildOrderItemsString(order.getOrderDetails()));
            }

            ExcelSheetBuilder.autosizeColumns(sheet, headers.size());
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException ex) {
            log.error("Xuất Excel đơn hàng thất bại", ex);
            throw new ReportExportException("Không thể xuất báo cáo đơn hàng", ex);
        }
    }

    public ByteArrayInputStream exportInventory() {
        List<Ingredient> ingredients = ingredientRepository.findAll();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            List<String> headers = Arrays.asList(
                    "ID", "Name", "Unit", "Quantity", "Reorder Level", "Unit Price", "Total Value", "Status");
            Sheet sheet = ExcelSheetBuilder.createSheetWithHeader(workbook, "Inventory", headers);

            int rowIdx = 1;
            for (Ingredient ingredient : ingredients) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(ingredient.getId());
                row.createCell(1).setCellValue(ingredient.getName());
                row.createCell(2).setCellValue(ingredient.getUnit());
                row.createCell(3).setCellValue(toDouble(ingredient.getQuantityOnHand()));
                row.createCell(4).setCellValue(toDouble(ingredient.getReorderLevel()));
                row.createCell(5).setCellValue(0.0);
                row.createCell(6).setCellValue(0.0);
                String status = ingredient.getQuantityOnHand() != null && ingredient.getReorderLevel() != null
                        && ingredient.getQuantityOnHand().compareTo(ingredient.getReorderLevel()) <= 0
                        ? "Low Stock" : "In Stock";
                row.createCell(7).setCellValue(status);
            }

            ExcelSheetBuilder.autosizeColumns(sheet, headers.size());
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException ex) {
            log.error("Xuất Excel tồn kho thất bại", ex);
            throw new ReportExportException("Không thể xuất báo cáo tồn kho", ex);
        }
    }

    public ByteArrayInputStream exportExpenses(LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = expenseRepository.findByExpenseDateBetween(startDate, endDate, Pageable.unpaged()).getContent();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            List<String> headers = Arrays.asList(
                    "ID", "Date", "Category", "Description", "Amount", "User");
            Sheet sheet = ExcelSheetBuilder.createSheetWithHeader(workbook, "Expenses", headers);

            int rowIdx = 1;
            for (Expense expense : expenses) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(expense.getId());
                row.createCell(1).setCellValue(expense.getExpenseDate() != null ? expense.getExpenseDate().toString() : "");
                row.createCell(2).setCellValue(expense.getCategory() != null ? expense.getCategory() : "");
                row.createCell(3).setCellValue(expense.getDescription() != null ? expense.getDescription() : "");
                row.createCell(4).setCellValue(toDouble(expense.getAmount()));
                row.createCell(5).setCellValue(expense.getUser() != null ? expense.getUser().getUsername() : "");
            }

            ExcelSheetBuilder.autosizeColumns(sheet, headers.size());
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException ex) {
            log.error("Xuất Excel chi phí thất bại", ex);
            throw new ReportExportException("Không thể xuất báo cáo chi phí", ex);
        }
    }

    private double toDouble(Number number) {
        return number != null ? number.doubleValue() : 0.0;
    }

    private String buildOrderItemsString(java.util.Collection<OrderDetail> orderDetails) {
        if (orderDetails == null || orderDetails.isEmpty()) {
            return "";
        }
        return orderDetails.stream()
                .filter(detail -> detail.getProduct() != null)
                .map(detail -> detail.getProduct().getName() + " (x" + detail.getQuantity() + ")")
                .collect(Collectors.joining(", "));
    }

    private void populateDateCell(Row row, int columnIndex, LocalDateTime value, CellStyle style) {
        if (value == null) {
            return;
        }
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private CellStyle buildDateTimeCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper helper = workbook.getCreationHelper();
        style.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        return style;
    }
}
