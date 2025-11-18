package com.giapho.coffee_shop_backend.service.report.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Collection;
import java.util.List;

/**
 * Helper để chuẩn hoá tạo sheet, header và style khi xuất Excel.
 */
final class ExcelSheetBuilder {

    private ExcelSheetBuilder() {}

    static Sheet createSheetWithHeader(Workbook workbook, String sheetName, Collection<String> headers) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row headerRow = sheet.createRow(0);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        int index = 0;
        for (String header : headers) {
            Cell cell = headerRow.createCell(index++);
            cell.setCellValue(header);
            cell.setCellStyle(headerStyle);
        }
        return sheet;
    }

    static void autosizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
