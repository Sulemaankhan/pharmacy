package com.pharmacy.drugstore.repository;

import com.pharmacy.drugstore.entity.EmailNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailNotificationLogRepository extends JpaRepository<EmailNotificationLog, Long> {
}
