package com.pharmacy.drugstore.export;

public enum ExportFormat {
    PDF,
    EXCEL;

    public static ExportFormat from(String raw) {
        if (raw == null || raw.isBlank()) {
            return PDF;
        }
        String value = raw.trim().toUpperCase();
        if ("XLSX".equals(value) || "XLS".equals(value) || "EXCEL".equals(value)) {
            return EXCEL;
        }
        if ("PDF".equals(value)) {
            return PDF;
        }
        throw new IllegalArgumentException("Supported export formats: pdf, excel");
    }
}
