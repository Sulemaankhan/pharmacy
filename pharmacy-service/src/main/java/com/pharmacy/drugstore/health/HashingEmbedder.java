package com.pharmacy.drugstore.health;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HashingEmbedder {
    public static final int DIM = 96;

    private HashingEmbedder() {}

    public static float[] embed(String text) {
        float[] vector = new float[DIM];
        if (text == null || text.isBlank()) return vector;
        List<String> tokens = tokens(text);
        if (tokens.isEmpty()) return vector;
        for (int i = 0; i < tokens.size(); i++) {
            add(vector, tokens.get(i), 1f);
            if (i + 1 < tokens.size()) {
                add(vector, tokens.get(i) + "_" + tokens.get(i + 1), 1.4f);
            }
        }
        normalize(vector);
        return vector;
    }

    public static double cosine(float[] left, float[] right) {
        if (left == null || right == null || left.length != right.length) return 0;
        double sum = 0;
        for (int i = 0; i < left.length; i++) sum += left[i] * right[i];
        return sum;
    }

    public static String encode(float[] vector) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format(Locale.US, "%.4f", vector[i]));
        }
        return sb.toString();
    }

    public static float[] decode(String raw) {
        float[] vector = new float[DIM];
        if (raw == null || raw.isBlank()) return vector;
        String[] parts = raw.split(",");
        int n = Math.min(DIM, parts.length);
        for (int i = 0; i < n; i++) {
            try {
                vector[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException ignored) {
                vector[i] = 0f;
            }
        }
        return vector;
    }

    static List<String> tokens(String text) {
        String[] parts = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]+", " ").split("\\s+");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            if (part.length() < 2 || part.equals("the") || part.equals("and") || part.equals("for")) continue;
            out.add(part);
        }
        return out;
    }

    private static void add(float[] vector, String token, float weight) {
        int slot = Math.floorMod(token.hashCode(), DIM);
        vector[slot] += weight;
    }

    private static void normalize(float[] vector) {
        double sum = 0;
        for (float v : vector) sum += v * v;
        if (sum <= 0) return;
        float scale = (float) (1.0 / Math.sqrt(sum));
        for (int i = 0; i < vector.length; i++) vector[i] *= scale;
    }
}
