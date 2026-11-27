package com.pharmacy.drugstore.repository;


import java.util.List;
import java.util.Optional;
import com.pharmacy.drugstore.entity.HealthReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthReportRepository extends JpaRepository<HealthReport, Long> {
    List<HealthReport> findByUser_IdOrderByUploadedAtDesc(Long userId);
    Optional<HealthReport> findByIdAndUser_Id(Long id, Long userId);
}
