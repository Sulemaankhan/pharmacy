package com.pharmacy.drugstore.health;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Locale;

public final class PdfReportReader {
    public record ExtractedPdf(String text, int pageCount) {}

    private PdfReportReader() {}

    public static ExtractedPdf read(MultipartFile file, int maxPages, int maxChars) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a PDF lab report to upload");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".pdf") && !type.contains("pdf")) {
            throw new IllegalArgumentException("Upload a PDF file");
        }
        PdfReader reader = new PdfReader(file.getBytes());
        try {
            if (reader.isEncrypted()) {
                throw new IllegalArgumentException("This PDF is password protected");
            }
            int pages = reader.getNumberOfPages();
            if (pages > maxPages) {
                throw new IllegalArgumentException("Please upload a report of " + maxPages + " pages or fewer");
            }
            StringBuilder text = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int page = 1; page <= pages; page++) {
                String pageText = extractor.getTextFromPage(page);
                if (pageText != null && !pageText.isBlank()) {
                    text.append(pageText).append('\n');
                }
                if (text.length() > maxChars) break;
            }
            String body = text.toString().trim();
            if (body.length() < 40) {
                throw new IllegalArgumentException("Could not read text from this PDF. Upload a text-based report, not a scanned image.");
            }
            if (body.length() > maxChars) body = body.substring(0, maxChars);
            return new ExtractedPdf(body, pages);
        } finally {
            reader.close();
        }
    }
}
