package com.pharmacy.drugstore.repository;


import java.util.List;
import com.pharmacy.drugstore.entity.HealthReportChunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthReportChunkRepository extends JpaRepository<HealthReportChunk, Long> {
    List<HealthReportChunk> findByReport_IdOrderByChunkIndexAsc(Long reportId);
}
