package com.pharmacy.notification.repository;

import com.pharmacy.notification.entity.EmailNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailNotificationLogRepository extends JpaRepository<EmailNotificationLog, Long> {
}
