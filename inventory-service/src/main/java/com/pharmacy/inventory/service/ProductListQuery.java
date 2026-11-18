package com.pharmacy.inventory.service;

public record ProductListQuery(Long categoryId, String q, boolean deals, boolean featured, String labPanel) {
    public static ProductListQuery from(Long categoryId, String q, Boolean deals, Boolean featured, String labPanel) {
        return new ProductListQuery(
                categoryId,
                normalizeQuery(q),
                Boolean.TRUE.equals(deals),
                Boolean.TRUE.equals(featured),
                normalizePanel(labPanel));
    }

    private static String normalizeQuery(String q) {
        if (q == null) {
            return null;
        }
        String trimmed = q.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizePanel(String labPanel) {
        if (labPanel == null) {
            return null;
        }
        String trimmed = labPanel.trim().toUpperCase();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
