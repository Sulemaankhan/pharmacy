package com.pharmacy.drugstore.dto;

public record HealthUploadResponse(
        HealthReportResponse report,
        HealthStatusResponse status
) {}
