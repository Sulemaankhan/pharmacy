package com.pharmacy.drugstore.repository;

import com.pharmacy.drugstore.entity.HealthReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HealthReportRepository extends JpaRepository<HealthReport, Long> {
    List<HealthReport> findByUser_IdOrderByUploadedAtDesc(Long userId);
    Optional<HealthReport> findByIdAndUser_Id(Long id, Long userId);
}
