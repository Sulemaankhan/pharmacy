package com.pharmacy.drugstore.export;

import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.OrderItem;
import com.pharmacy.drugstore.entity.User;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
public class ExcelOrderHistoryExporter extends AbstractOrderHistoryExporter {
    @Override
    public ExportFormat format() {
        return ExportFormat.EXCEL;
    }

    @Override
    protected ExportFile write(User user, List<CustomerOrder> orders) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle title = titleStyle(workbook);
            CellStyle header = headerStyle(workbook);
            CellStyle text = textStyle(workbook);
            CellStyle money = moneyStyle(workbook);

            Sheet summary = workbook.createSheet("Order History");
            int rowIdx = 0;
            Row titleRow = summary.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Medicine Drugstore — Order History");
            titleCell.setCellStyle(title);

            Row meta = summary.createRow(rowIdx++);
            meta.createCell(0).setCellValue("Customer");
            meta.createCell(1).setCellValue(user.getName() + " (" + user.getEmail() + ")");
            Row generated = summary.createRow(rowIdx++);
            generated.createCell(0).setCellValue("Generated");
            generated.createCell(1).setCellValue(DATE_TIME.format(Instant.now()));
            rowIdx++;

            Row headerRow = summary.createRow(rowIdx++);
            String[] columns = {
                    "Order number", "Placed at", "Status", "Payment mode", "Transaction ref",
                    "Items", "Subtotal (INR)", "Shipping (INR)", "Total (INR)", "Products"
            };
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(header);
            }

            BigDecimal grandTotal = BigDecimal.ZERO;
            for (CustomerOrder order : orders) {
                Row row = summary.createRow(rowIdx++);
                writeText(row, 0, order.getOrderNumber(), text);
                writeText(row, 1, order.getCreatedAt() == null ? "-" : DATE_TIME.format(order.getCreatedAt()), text);
                writeText(row, 2, order.getStatus() == null ? "-" : order.getStatus().name(), text);
                writeText(row, 3, paymentMode(order), text);
                writeText(row, 4, txnRef(order), text);
                writeNumber(row, 5, order.getItems() == null ? 0 : order.getItems().size(), text);
                writeMoney(row, 6, order.getSubtotal(), money);
                writeMoney(row, 7, order.getShipping(), money);
                writeMoney(row, 8, order.getTotal(), money);
                writeText(row, 9, itemsSummary(order), text);
                if (order.getTotal() != null) {
                    grandTotal = grandTotal.add(order.getTotal());
                }
            }

            Row totalRow = summary.createRow(rowIdx);
            writeText(totalRow, 7, "Grand total", header);
            writeMoney(totalRow, 8, grandTotal, money);

            for (int i = 0; i < columns.length; i++) {
                summary.autoSizeColumn(i);
            }

            Sheet itemsSheet = workbook.createSheet("Line Items");
            Row itemHeader = itemsSheet.createRow(0);
            String[] itemColumns = {"Order number", "Product", "Quantity", "Unit price (INR)", "Line total (INR)"};
            for (int i = 0; i < itemColumns.length; i++) {
                Cell cell = itemHeader.createCell(i);
                cell.setCellValue(itemColumns[i]);
                cell.setCellStyle(header);
            }
            int itemRow = 1;
            for (CustomerOrder order : orders) {
                for (OrderItem item : order.getItems()) {
                    Row row = itemsSheet.createRow(itemRow++);
                    writeText(row, 0, order.getOrderNumber(), text);
                    writeText(row, 1, item.getProductName(), text);
                    writeNumber(row, 2, item.getQuantity(), text);
                    writeMoney(row, 3, item.getUnitPrice(), money);
                    writeMoney(row, 4, lineAmount(item), money);
                }
            }
            for (int i = 0; i < itemColumns.length; i++) {
                itemsSheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ExportFile(
                    out.toByteArray(),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "order-history-" + fileStamp() + ".xlsx");
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate Excel order history", ex);
        }
    }

    private void writeText(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "-" : value);
        cell.setCellStyle(style);
    }

    private void writeNumber(Row row, int col, int value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void writeMoney(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? 0d : value.doubleValue());
        cell.setCellStyle(style);
    }

    private CellStyle titleStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle textStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        return style;
    }

    private CellStyle moneyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        return style;
    }
}
