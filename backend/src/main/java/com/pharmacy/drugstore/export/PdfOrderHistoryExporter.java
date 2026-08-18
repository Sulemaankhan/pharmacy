package com.pharmacy.drugstore.export;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.OrderItem;
import com.pharmacy.drugstore.entity.User;
import org.springframework.stereotype.Component;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
public class PdfOrderHistoryExporter extends AbstractOrderHistoryExporter {
    private static final Color BLUE = new Color(13, 78, 192);
    private static final Color HEADER_BG = new Color(232, 241, 255);
    private static final Color MUTED = new Color(100, 116, 139);

    @Override
    public ExportFormat format() {
        return ExportFormat.PDF;
    }

    @Override
    protected ExportFile write(User user, List<CustomerOrder> orders) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, BLUE);
            Font metaFont = new Font(Font.HELVETICA, 10, Font.NORMAL, MUTED);
            Font headFont = new Font(Font.HELVETICA, 9, Font.BOLD, BLUE);
            Font cellFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
            Font boldFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.DARK_GRAY);

            document.add(new Paragraph("Medicine Drugstore", titleFont));
            document.add(new Paragraph("Order history", new Font(Font.HELVETICA, 12, Font.BOLD, Color.DARK_GRAY)));
            document.add(new Paragraph(
                    "Customer: " + user.getName() + "  |  " + user.getEmail()
                            + "  |  Generated: " + DATE_TIME.format(Instant.now())
                            + "  |  Orders: " + orders.size(),
                    metaFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{2.1f, 2.2f, 1.3f, 1.5f, 1.7f, 1.3f, 1.3f, 4.2f});
            table.setWidthPercentage(100);
            String[] headers = {"Order", "Placed", "Status", "Payment", "Txn ref", "Items", "Total (INR)", "Products"};
            for (String header : headers) {
                table.addCell(headerCell(header, headFont));
            }

            BigDecimal grandTotal = BigDecimal.ZERO;
            for (CustomerOrder order : orders) {
                table.addCell(bodyCell(order.getOrderNumber(), cellFont));
                table.addCell(bodyCell(order.getCreatedAt() == null ? "-" : DATE_TIME.format(order.getCreatedAt()), cellFont));
                table.addCell(bodyCell(order.getStatus() == null ? "-" : order.getStatus().name(), cellFont));
                table.addCell(bodyCell(paymentMode(order), cellFont));
                table.addCell(bodyCell(txnRef(order), cellFont));
                table.addCell(bodyCell(String.valueOf(order.getItems() == null ? 0 : order.getItems().size()), cellFont));
                table.addCell(bodyCell(money(order.getTotal()), boldFont));
                table.addCell(bodyCell(itemsSummary(order), cellFont));
                if (order.getTotal() != null) {
                    grandTotal = grandTotal.add(order.getTotal());
                }
            }

            if (orders.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("No orders found for this account.", cellFont));
                empty.setColspan(8);
                empty.setPadding(10);
                empty.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(empty);
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Grand total (INR): " + money(grandTotal), boldFont));

            if (!orders.isEmpty()) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Line items", new Font(Font.HELVETICA, 12, Font.BOLD, Color.DARK_GRAY)));
                document.add(new Paragraph(" "));
                PdfPTable items = new PdfPTable(new float[]{2.2f, 4.5f, 1.2f, 1.5f, 1.6f});
                items.setWidthPercentage(100);
                for (String header : List.of("Order", "Product", "Qty", "Unit (INR)", "Line (INR)")) {
                    items.addCell(headerCell(header, headFont));
                }
                for (CustomerOrder order : orders) {
                    for (OrderItem item : order.getItems()) {
                        items.addCell(bodyCell(order.getOrderNumber(), cellFont));
                        items.addCell(bodyCell(item.getProductName(), cellFont));
                        items.addCell(bodyCell(String.valueOf(item.getQuantity()), cellFont));
                        items.addCell(bodyCell(money(item.getUnitPrice()), cellFont));
                        items.addCell(bodyCell(money(lineAmount(item)), cellFont));
                    }
                }
                document.add(items);
            }

            document.close();
            return new ExportFile(
                    out.toByteArray(),
                    "application/pdf",
                    "order-history-" + fileStamp() + ".pdf");
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate PDF order history", ex);
        }
    }

    private PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell bodyCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "-" : text, font));
        cell.setPadding(5);
        return cell;
    }
}
