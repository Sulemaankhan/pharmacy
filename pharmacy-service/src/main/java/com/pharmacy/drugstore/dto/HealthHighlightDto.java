package com.pharmacy.drugstore.dto;

public record HealthHighlightDto(
        String panel,
        String snippet,
        Double score
) {}
