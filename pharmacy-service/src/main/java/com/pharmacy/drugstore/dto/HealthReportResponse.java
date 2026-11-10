package com.pharmacy.drugstore.dto;

import java.time.Instant;
import java.util.List;

public record HealthReportResponse(
        Long id,
        Long userId,
        String fileName,
        Instant uploadedAt,
        Integer pageCount,
        Integer chunkCount,
        Integer overallScore,
        String overallStatus,
        String summary,
        List<HealthMetricDto> metrics,
        List<HealthHighlightDto> highlights
) {}
