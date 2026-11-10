package com.pharmacy.drugstore.dto;

import java.time.Instant;

public record HealthHistoryDto(
        Long reportId,
        String fileName,
        Instant uploadedAt,
        Integer overallScore,
        String overallStatus
) {}
