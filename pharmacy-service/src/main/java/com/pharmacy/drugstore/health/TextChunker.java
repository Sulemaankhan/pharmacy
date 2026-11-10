package com.pharmacy.drugstore.health;

import java.util.ArrayList;
import java.util.List;

public final class TextChunker {
    private TextChunker() {}

    public static List<String> chunk(String text, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null) return chunks;
        String clean = text.replace('\u0000', ' ').replaceAll("[ \\t]+", " ").trim();
        if (clean.isEmpty()) return chunks;
        if (clean.length() <= size) {
            chunks.add(clean);
            return chunks;
        }
        int start = 0;
        while (start < clean.length()) {
            int end = Math.min(clean.length(), start + size);
            if (end < clean.length()) {
                int breakAt = clean.lastIndexOf('\n', end);
                if (breakAt <= start + size / 2) breakAt = clean.lastIndexOf(' ', end);
                if (breakAt > start + size / 3) end = breakAt;
            }
            String piece = clean.substring(start, end).trim();
            if (piece.length() > 40) chunks.add(piece.length() > 1800 ? piece.substring(0, 1800) : piece);
            if (end >= clean.length()) break;
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }
}
