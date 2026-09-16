package com.pharmacy.drugstore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "health_metrics")
public class HealthMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private HealthReport report;

    @Column(length = 32)
    private String panel;

    @Column(length = 32)
    private String code;

    private String name;
    private Double value;
    private String unit;
    private String status;
    private Double minNormal;
    private Double maxNormal;
    private Instant recordedAt = Instant.now();

    public Long getUserId() {
        return user == null ? null : user.getId();
    }

    public Long getReportId() {
        return report == null ? null : report.getId();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public HealthReport getReport() { return report; }
    public void setReport(HealthReport report) { this.report = report; }
    public String getPanel() { return panel; }
    public void setPanel(String panel) { this.panel = panel; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getMinNormal() { return minNormal; }
    public void setMinNormal(Double minNormal) { this.minNormal = minNormal; }
    public Double getMaxNormal() { return maxNormal; }
    public void setMaxNormal(Double maxNormal) { this.maxNormal = maxNormal; }
    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
}
