package com.pharmacy.drugstore.service;

import com.pharmacy.drugstore.dto.HealthHighlightDto;
import com.pharmacy.drugstore.dto.HealthHistoryDto;
import com.pharmacy.drugstore.dto.HealthMetricDto;
import com.pharmacy.drugstore.dto.HealthPanelScoreDto;
import com.pharmacy.drugstore.dto.HealthReportResponse;
import com.pharmacy.drugstore.dto.HealthStatusResponse;
import com.pharmacy.drugstore.dto.HealthUploadResponse;
import com.pharmacy.drugstore.entity.HealthMetric;
import com.pharmacy.drugstore.entity.HealthReport;
import com.pharmacy.drugstore.entity.HealthReportChunk;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.health.HashingEmbedder;
import com.pharmacy.drugstore.health.HealthSummaryBuilder;
import com.pharmacy.drugstore.health.LabCatalog;
import com.pharmacy.drugstore.health.PdfReportReader;
import com.pharmacy.drugstore.health.RagRetriever;
import com.pharmacy.drugstore.health.TextChunker;
import com.pharmacy.drugstore.repository.HealthReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HealthReportService {
    private static final Logger log = LoggerFactory.getLogger(HealthReportService.class);
    private final HealthReportRepository reports;
    private final int maxPages;
    private final int maxChars;

    public HealthReportService(
            HealthReportRepository reports,
            @Value("${app.health.max-pages:20}") int maxPages,
            @Value("${app.health.max-chars:80000}") int maxChars) {
        this.reports = reports;
        this.maxPages = maxPages;
        this.maxChars = maxChars;
    }

    @Transactional
    public HealthUploadResponse upload(User user, MultipartFile file) {
        PdfReportReader.ExtractedPdf extracted;
        try {
            extracted = PdfReportReader.read(file, maxPages, maxChars);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (IOException ex) {
            log.warn("Health report PDF read failed userId={} reason={}", user.getId(), ex.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read this PDF");
        }

        HealthReport report = new HealthReport();
        report.setUser(user);
        report.setFileName(safeName(file.getOriginalFilename()));
        report.setPageCount(extracted.pageCount());

        List<String> pieces = TextChunker.chunk(extracted.text(), 900, 140);
        int index = 0;
        for (String piece : pieces) {
            HealthReportChunk chunk = new HealthReportChunk();
            chunk.setReport(report);
            chunk.setChunkIndex(index++);
            chunk.setContent(piece);
            chunk.setEmbedding(HashingEmbedder.encode(HashingEmbedder.embed(piece)));
            report.getChunks().add(chunk);
        }
        report.setChunkCount(report.getChunks().size());

        List<RagRetriever.Hit> hits = RagRetriever.retrieve(report.getChunks());
        List<LabCatalog.Extracted> found = LabCatalog.extract(extracted.text());
        for (LabCatalog.Extracted item : found) {
            HealthMetric metric = new HealthMetric();
            metric.setUser(user);
            metric.setReport(report);
            metric.setPanel(item.spec.panel);
            metric.setCode(item.spec.code);
            metric.setName(item.spec.name);
            metric.setValue(item.value);
            metric.setUnit(item.spec.unit);
            metric.setStatus(item.status);
            metric.setMinNormal(item.min);
            metric.setMaxNormal(item.max);
            metric.setRecordedAt(report.getUploadedAt());
            report.getMetrics().add(metric);
        }

        int score = overallScore(found);
        String status = LabCatalog.band(score);
        report.setOverallScore(score);
        report.setOverallStatus(status);
        report.setSummary(HealthSummaryBuilder.build(report.getFileName(), score, status, found, hits));
        reports.save(report);

        log.info("Health report uploaded userId={} reportId={} file={} pages={} chunks={} metrics={} score={}",
                user.getId(), report.getId(), report.getFileName(), report.getPageCount(),
                report.getChunkCount(), found.size(), score);

        HealthReportResponse body = toReport(report, true, hits);
        return new HealthUploadResponse(body, status(user));
    }

    @Transactional(readOnly = true)
    public List<HealthReportResponse> list(User user) {
        List<HealthReportResponse> out = new ArrayList<>();
        for (HealthReport report : reports.findByUser_IdOrderByUploadedAtDesc(user.getId())) {
            out.add(toReport(report, false, List.of()));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public HealthReportResponse get(User user, Long id) {
        HealthReport report = reports.findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Health report not found"));
        List<RagRetriever.Hit> hits = RagRetriever.retrieve(report.getChunks());
        return toReport(report, true, hits);
    }

    @Transactional(readOnly = true)
    public HealthStatusResponse status(User user) {
        List<HealthReport> history = reports.findByUser_IdOrderByUploadedAtDesc(user.getId());
        HealthReport latest = history.isEmpty() ? null : history.get(0);
        List<HealthMetric> latestMetrics = latest == null ? List.of() : latest.getMetrics();
        Map<String, List<HealthMetric>> byPanel = new LinkedHashMap<>();
        for (String panel : LabCatalog.PANELS) byPanel.put(panel, new ArrayList<>());
        for (HealthMetric metric : latestMetrics) {
            byPanel.computeIfAbsent(metric.getPanel(), key -> new ArrayList<>()).add(metric);
        }
        List<HealthPanelScoreDto> panels = new ArrayList<>();
        for (String panel : LabCatalog.PANELS) {
            List<HealthMetric> group = byPanel.getOrDefault(panel, List.of());
            Integer score = panelScore(group);
            String band = score == null ? "NO_DATA" : LabCatalog.band(score);
            panels.add(new HealthPanelScoreDto(
                    panel,
                    RagRetriever.panelName(panel),
                    score,
                    band,
                    finding(group)
            ));
        }
        List<HealthMetricDto> metricDtos = new ArrayList<>();
        for (HealthMetric metric : latestMetrics) metricDtos.add(toMetric(metric));
        List<HealthHistoryDto> historyDtos = new ArrayList<>();
        for (HealthReport report : history) {
            historyDtos.add(new HealthHistoryDto(
                    report.getId(),
                    report.getFileName(),
                    report.getUploadedAt(),
                    report.getOverallScore(),
                    report.getOverallStatus()
            ));
        }
        return new HealthStatusResponse(
                latest == null ? null : latest.getOverallScore(),
                latest == null ? "NO_DATA" : latest.getOverallStatus(),
                latest == null ? null : latest.getSummary(),
                latest == null ? null : latest.getId(),
                panels,
                metricDtos,
                historyDtos
        );
    }

    private HealthReportResponse toReport(HealthReport report, boolean detail, List<RagRetriever.Hit> hits) {
        List<HealthMetricDto> metricDtos = new ArrayList<>();
        List<HealthHighlightDto> highlights = new ArrayList<>();
        if (detail) {
            for (HealthMetric metric : report.getMetrics()) metricDtos.add(toMetric(metric));
            for (RagRetriever.Hit hit : hits) {
                highlights.add(new HealthHighlightDto(hit.panel, hit.snippet, hit.score));
            }
        }
        return new HealthReportResponse(
                report.getId(),
                report.getUserId(),
                report.getFileName(),
                report.getUploadedAt(),
                report.getPageCount(),
                report.getChunkCount(),
                report.getOverallScore(),
                report.getOverallStatus(),
                report.getSummary(),
                metricDtos,
                highlights
        );
    }

    private HealthMetricDto toMetric(HealthMetric metric) {
        int score = LabCatalog.score(
                metric.getValue() == null ? 0 : metric.getValue(),
                metric.getMinNormal(),
                metric.getMaxNormal(),
                metric.getCode()
        );
        return new HealthMetricDto(
                metric.getId(),
                metric.getPanel(),
                metric.getCode(),
                metric.getName(),
                metric.getValue(),
                metric.getUnit(),
                metric.getStatus(),
                score,
                metric.getMinNormal(),
                metric.getMaxNormal(),
                metric.getRecordedAt()
        );
    }

    private Integer panelScore(List<HealthMetric> group) {
        if (group == null || group.isEmpty()) return null;
        int sum = 0;
        int count = 0;
        for (HealthMetric metric : group) {
            if (metric.getValue() == null) continue;
            sum += LabCatalog.score(metric.getValue(), metric.getMinNormal(), metric.getMaxNormal(), metric.getCode());
            count++;
        }
        return count == 0 ? null : Math.round(sum / (float) count);
    }

    private String finding(List<HealthMetric> group) {
        if (group == null || group.isEmpty()) return "No values in the latest report";
        List<String> odd = new ArrayList<>();
        for (HealthMetric metric : group) {
            if (metric.getStatus() != null && !"NORMAL".equals(metric.getStatus())) {
                odd.add(metric.getName() + " " + metric.getStatus().toLowerCase());
            }
        }
        if (odd.isEmpty()) return "Values are in the typical range";
        return String.join(", ", odd);
    }

    private int overallScore(List<LabCatalog.Extracted> found) {
        if (found == null || found.isEmpty()) return 55;
        int sum = 0;
        for (LabCatalog.Extracted item : found) sum += item.score;
        return Math.round(sum / (float) found.size());
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) return "health-report.pdf";
        String base = name.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) base = base.substring(slash + 1);
        return base.length() > 180 ? base.substring(base.length() - 180) : base;
    }
}
