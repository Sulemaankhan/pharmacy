package com.pharmacy.drugstore.health;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HealthSummaryBuilder {
    private HealthSummaryBuilder() {}

    public static String build(String fileName, int score, String status,
                               List<LabCatalog.Extracted> metrics, List<RagRetriever.Hit> hits) {
        StringBuilder sb = new StringBuilder();
        sb.append("RAG summary of ").append(fileName == null ? "uploaded report" : fileName).append(". ");
        sb.append("Overall profile status: ").append(label(status)).append(" (").append(score).append("/100). ");
        Map<String, List<LabCatalog.Extracted>> byPanel = new LinkedHashMap<>();
        for (String panel : LabCatalog.PANELS) byPanel.put(panel, new ArrayList<>());
        byPanel.put("GENERAL", new ArrayList<>());
        for (LabCatalog.Extracted metric : metrics) {
            byPanel.computeIfAbsent(metric.spec.panel, key -> new ArrayList<>()).add(metric);
        }
        boolean any = false;
        for (String panel : LabCatalog.PANELS) {
            List<LabCatalog.Extracted> group = byPanel.get(panel);
            if (group == null || group.isEmpty()) continue;
            any = true;
            sb.append(RagRetriever.panelName(panel)).append(": ");
            for (int i = 0; i < group.size(); i++) {
                LabCatalog.Extracted item = group.get(i);
                if (i > 0) sb.append("; ");
                sb.append(item.spec.name).append(' ').append(trim(item.value)).append(' ')
                        .append(item.spec.unit).append(" (").append(item.status.toLowerCase()).append(')');
            }
            sb.append(". ");
        }
        if (!any) {
            sb.append("No standard lab values were detected. The summary uses the most relevant retrieved passages. ");
        }
        if (!hits.isEmpty()) {
            RagRetriever.Hit top = hits.get(0);
            sb.append("Retrieved evidence (").append(RagRetriever.panelName(top.panel)).append("): ")
                    .append(top.snippet).append(' ');
        }
        sb.append("This is an automated reading of the uploaded PDF, not a medical diagnosis. Please consult a doctor.");
        String text = sb.toString().trim();
        return text.length() > 3900 ? text.substring(0, 3900) : text;
    }

    private static String label(String status) {
        if ("HIGH_RISK".equals(status)) return "needs attention";
        if ("ATTENTION".equals(status)) return "watch closely";
        return "within typical range";
    }

    private static String trim(double value) {
        if (Math.abs(value - Math.round(value)) < 0.0001) return String.valueOf(Math.round(value));
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
