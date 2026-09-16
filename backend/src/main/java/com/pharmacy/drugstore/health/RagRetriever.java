package com.pharmacy.drugstore.health;

import com.pharmacy.drugstore.entity.HealthReportChunk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class RagRetriever {
    public static final class Hit {
        public final String query;
        public final String panel;
        public final String snippet;
        public final double score;

        public Hit(String query, String panel, String snippet, double score) {
            this.query = query;
            this.panel = panel;
            this.snippet = snippet;
            this.score = score;
        }
    }

    private static final String[][] QUERIES = {
            {"THYROID", "thyroid tsh t3 t4 thyroxine hypothyroidism hyperthyroidism"},
            {"KIDNEY", "kidney creatinine urea uric kft gfr bun renal"},
            {"LIVER", "liver lft sgot sgpt alt ast bilirubin alp albumin hepatic"},
            {"HEART", "heart lipid cholesterol hdl ldl triglyceride cardiac"},
            {"DIABETES", "diabetes hba1c glucose fasting sugar insulin glycemic"},
            {"OVERALL", "impression summary finding abnormal high low reference range"}
    };

    private RagRetriever() {}

    public static List<Hit> retrieve(List<HealthReportChunk> chunks) {
        List<Hit> hits = new ArrayList<>();
        if (chunks == null || chunks.isEmpty()) return hits;
        for (String[] query : QUERIES) {
            float[] qVec = HashingEmbedder.embed(query[1]);
            HealthReportChunk best = null;
            double bestScore = 0.08;
            for (HealthReportChunk chunk : chunks) {
                double score = HashingEmbedder.cosine(qVec, HashingEmbedder.decode(chunk.getEmbedding()));
                if (score > bestScore) {
                    bestScore = score;
                    best = chunk;
                }
            }
            if (best != null) {
                hits.add(new Hit(query[1], query[0], snippet(best.getContent()), round(bestScore)));
            }
        }
        hits.sort(Comparator.comparingDouble((Hit h) -> h.score).reversed());
        return hits;
    }

    private static String snippet(String content) {
        if (content == null) return "";
        String clean = content.replaceAll("\\s+", " ").trim();
        return clean.length() > 280 ? clean.substring(0, 277) + "..." : clean;
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public static String panelName(String panel) {
        if (panel == null) return "General";
        switch (panel.toUpperCase(Locale.ROOT)) {
            case "THYROID": return "Thyroid";
            case "KIDNEY": return "Kidney";
            case "LIVER": return "Liver";
            case "HEART": return "Heart";
            case "DIABETES": return "Diabetes";
            default: return "Overall";
        }
    }
}
