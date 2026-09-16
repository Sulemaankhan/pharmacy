package com.pharmacy.drugstore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "health_report_chunks")
public class HealthReportChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private HealthReport report;

    private Integer chunkIndex;

    @Column(length = 2000, nullable = false)
    private String content;

    @Column(length = 2000)
    private String embedding;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public HealthReport getReport() { return report; }
    public void setReport(HealthReport report) { this.report = report; }
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
}
