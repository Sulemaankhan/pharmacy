package com.pharmacy.drugstore.health;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LabCatalog {
    public static final class Spec {
        public final String code;
        public final String name;
        public final String panel;
        public final String unit;
        public final double min;
        public final double max;
        public final Pattern pattern;

        Spec(String code, String name, String panel, String unit, double min, double max, String aliases) {
            this.code = code;
            this.name = name;
            this.panel = panel;
            this.unit = unit;
            this.min = min;
            this.max = max;
            this.pattern = Pattern.compile("(?is)(?<!\\w)(?:" + aliases + ")\\s*[:\\-–]?[^\\n\\r]{0,48}?([0-9]+(?:\\.[0-9]+)?)");
        }
    }

    public static final class Extracted {
        public final Spec spec;
        public final double value;
        public final Double min;
        public final Double max;
        public final String status;
        public final int score;

        Extracted(Spec spec, double value, Double min, Double max, String status, int score) {
            this.spec = spec;
            this.value = value;
            this.min = min;
            this.max = max;
            this.status = status;
            this.score = score;
        }
    }

    private static final Pattern RANGE = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*[-–to]{1,3}\\s*([0-9]+(?:\\.[0-9]+)?)");

    private static final List<Spec> SPECS = List.of(
            new Spec("TSH", "TSH", "THYROID", "µIU/mL", 0.4, 4.0, "tsh(?:\\s*ultrasensitive)?"),
            new Spec("FT4", "Free T4", "THYROID", "ng/dL", 0.8, 1.8, "free\\s*t4|ft4"),
            new Spec("FT3", "Free T3", "THYROID", "pg/mL", 2.3, 4.2, "free\\s*t3|ft3"),
            new Spec("T4", "Total T4", "THYROID", "µg/dL", 5.0, 12.0, "total\\s*t4|(?<!free )t4"),
            new Spec("T3", "Total T3", "THYROID", "ng/dL", 80, 200, "total\\s*t3|(?<!free )t3"),
            new Spec("CREATININE", "Serum Creatinine", "KIDNEY", "mg/dL", 0.6, 1.3, "serum\\s*creatinine|creatinine"),
            new Spec("UREA", "Urea", "KIDNEY", "mg/dL", 15, 40, "blood\\s*urea|urea\\s*nitrogen|\\burea\\b"),
            new Spec("URIC", "Uric Acid", "KIDNEY", "mg/dL", 3.5, 7.2, "uric\\s*acid"),
            new Spec("EGFR", "eGFR", "KIDNEY", "mL/min", 90, 200, "egfr|estimated\\s*gfr"),
            new Spec("SGPT", "SGPT / ALT", "LIVER", "U/L", 7, 56, "sgpt|alt|alanine\\s*amino"),
            new Spec("SGOT", "SGOT / AST", "LIVER", "U/L", 8, 45, "sgot|ast|aspartate\\s*amino"),
            new Spec("BILI", "Bilirubin Total", "LIVER", "mg/dL", 0.2, 1.2, "total\\s*bilirubin|bilirubin"),
            new Spec("ALP", "Alkaline Phosphatase", "LIVER", "U/L", 44, 147, "alkaline\\s*phosphatase|\\balp\\b"),
            new Spec("CHOL", "Total Cholesterol", "HEART", "mg/dL", 125, 200, "total\\s*cholesterol|cholesterol"),
            new Spec("HDL", "HDL Cholesterol", "HEART", "mg/dL", 40, 80, "hdl"),
            new Spec("LDL", "LDL Cholesterol", "HEART", "mg/dL", 0, 100, "ldl"),
            new Spec("TRIG", "Triglycerides", "HEART", "mg/dL", 0, 150, "triglyceride"),
            new Spec("HBA1C", "HbA1c", "DIABETES", "%", 4.0, 5.6, "hba1c|glycated\\s*hemoglobin|a1c"),
            new Spec("FBS", "Fasting Blood Sugar", "DIABETES", "mg/dL", 70, 99, "fasting\\s*(?:blood\\s*)?(?:sugar|glucose)|fbs|fpg"),
            new Spec("PPBS", "Postprandial Sugar", "DIABETES", "mg/dL", 70, 140, "postprandial|ppbs|pp\\s*blood\\s*sugar"),
            new Spec("RBS", "Random Blood Sugar", "DIABETES", "mg/dL", 70, 140, "random\\s*(?:blood\\s*)?(?:sugar|glucose)|rbs"),
            new Spec("HB", "Hemoglobin", "GENERAL", "g/dL", 12, 17, "ha?emoglobin|\\bhgb\\b|\\bhb\\b"),
            new Spec("VITD", "Vitamin D", "GENERAL", "ng/mL", 30, 100, "vitamin\\s*d|25\\s*oh")
    );

    public static final List<String> PANELS = List.of("THYROID", "KIDNEY", "LIVER", "HEART", "DIABETES");

    private LabCatalog() {}

    public static List<Extracted> extract(String text) {
        List<Extracted> found = new ArrayList<>();
        if (text == null || text.isBlank()) return found;
        for (Spec spec : SPECS) {
            Matcher matcher = spec.pattern.matcher(text);
            if (!matcher.find()) continue;
            double value = Double.parseDouble(matcher.group(1));
            int from = Math.max(0, matcher.start());
            int to = Math.min(text.length(), matcher.end() + 40);
            String window = text.substring(from, to);
            Double min = spec.min;
            Double max = spec.max;
            Matcher range = RANGE.matcher(window);
            if (range.find()) {
                min = Double.parseDouble(range.group(1));
                max = Double.parseDouble(range.group(2));
                if (min > max) {
                    double swap = min;
                    min = max;
                    max = swap;
                }
            }
            String status = status(value, min, max, spec.code);
            found.add(new Extracted(spec, value, min, max, status, score(value, min, max, spec.code)));
        }
        found.sort(Comparator.comparing((Extracted e) -> PANELS.indexOf(e.spec.panel) < 0 ? 99 : PANELS.indexOf(e.spec.panel))
                .thenComparing(e -> e.spec.name));
        return found;
    }

    public static String status(double value, Double min, Double max, String code) {
        if ("EGFR".equals(code) || "HDL".equals(code)) {
            if (min != null && value < min) return "LOW";
            return "NORMAL";
        }
        if (min != null && value < min) return "LOW";
        if (max != null && value > max) return "HIGH";
        return "NORMAL";
    }

    public static int score(double value, Double min, Double max, String code) {
        if (min == null || max == null || max <= min) return 70;
        if ("EGFR".equals(code) || "HDL".equals(code)) {
            if (value >= min) return 96;
            double drop = (min - value) / Math.max(1, min) * 80;
            return clamp(Math.min(82, 100 - drop));
        }
        if (value >= min && value <= max) {
            return 96;
        }
        double range = max - min;
        double distance = value < min ? min - value : value - max;
        return clamp(100 - (distance / range) * 55);
    }

    public static String band(int score) {
        if (score >= 85) return "NORMAL";
        if (score >= 65) return "ATTENTION";
        return "HIGH_RISK";
    }

    private static int clamp(double value) {
        return (int) Math.round(Math.max(20, Math.min(100, value)));
    }
}
