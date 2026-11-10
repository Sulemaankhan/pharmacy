package com.pharmacy.drugstore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "health_reports")
public class HealthReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String fileName;

    private Integer pageCount;
    private Integer chunkCount;
    private Integer overallScore;
    private String overallStatus;
    private Instant uploadedAt = Instant.now();

    @Column(length = 4000)
    private String summary;

    @JsonIgnore
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HealthReportChunk> chunks = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HealthMetric> metrics = new ArrayList<>();

    public Long getUserId() {
        return user == null ? null : user.getId();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public Integer getOverallScore() { return overallScore; }
    public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }
    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<HealthReportChunk> getChunks() { return chunks; }
    public void setChunks(List<HealthReportChunk> chunks) { this.chunks = chunks; }
    public List<HealthMetric> getMetrics() { return metrics; }
    public void setMetrics(List<HealthMetric> metrics) { this.metrics = metrics; }
}
