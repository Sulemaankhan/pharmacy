package com.pharmacy.drugstore.dto;

public record HealthPanelScoreDto(
        String id,
        String name,
        Integer score,
        String status,
        String finding
) {}
