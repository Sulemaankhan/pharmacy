package com.pharmacy.drugstore.dto;

import java.util.List;

public record HealthStatusResponse(
        Integer overallScore,
        String overallStatus,
        String summary,
        Long latestReportId,
        List<HealthPanelScoreDto> panels,
        List<HealthMetricDto> metrics,
        List<HealthHistoryDto> history
) {}
