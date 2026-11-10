package com.pharmacy.drugstore.dto;

import java.time.Instant;
import java.util.List;

public record HealthMetricDto(
        Long id,
        String panel,
        String code,
        String name,
        Double value,
        String unit,
        String status,
        Integer score,
        Double minNormal,
        Double maxNormal,
        Instant recordedAt
) {}
