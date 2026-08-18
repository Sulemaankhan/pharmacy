package com.pharmacy.drugstore.export;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderExportFactory {
    private final Map<ExportFormat, OrderHistoryExporter> exporters = new EnumMap<>(ExportFormat.class);

    public OrderExportFactory(List<OrderHistoryExporter> implementations) {
        for (OrderHistoryExporter exporter : implementations) {
            exporters.put(exporter.format(), exporter);
        }
    }

    public OrderHistoryExporter create(ExportFormat format) {
        OrderHistoryExporter exporter = exporters.get(format);
        if (exporter == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported export format: " + format);
        }
        return exporter;
    }
}
