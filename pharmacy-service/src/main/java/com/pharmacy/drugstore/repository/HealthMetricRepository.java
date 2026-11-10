package com.pharmacy.drugstore.repository;

import com.pharmacy.drugstore.entity.HealthMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HealthMetricRepository extends JpaRepository<HealthMetric, Long> {
    List<HealthMetric> findByUser_IdOrderByRecordedAtDesc(Long userId);
    List<HealthMetric> findByReport_IdOrderByPanelAscNameAsc(Long reportId);
}
