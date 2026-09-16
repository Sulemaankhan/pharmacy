package com.pharmacy.drugstore.repository;

import com.pharmacy.drugstore.entity.HealthReportChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HealthReportChunkRepository extends JpaRepository<HealthReportChunk, Long> {
    List<HealthReportChunk> findByReport_IdOrderByChunkIndexAsc(Long reportId);
}
