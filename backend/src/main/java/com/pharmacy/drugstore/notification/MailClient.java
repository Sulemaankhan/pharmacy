package com.pharmacy.drugstore.notification;

import com.pharmacy.drugstore.entity.EmailNotificationLog;
import com.pharmacy.drugstore.repository.EmailNotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MailClient {
    private static final Logger log = LoggerFactory.getLogger(MailClient.class);
    private final EmailNotificationLogRepository logs;

    public MailClient(EmailNotificationLogRepository logs) {
        this.logs = logs;
    }

    public String send(String type, String to, String subject, String body) {
        log.info("Email [{}] to={} subject={}\n{}", type, to, subject, body);
        EmailNotificationLog row = new EmailNotificationLog();
        row.setType(type);
        row.setRecipient(to);
        row.setSubject(subject);
        row.setBody(body);
        row.setStatus("SENT");
        logs.save(row);
        return "Email queued to " + to;
    }
}
