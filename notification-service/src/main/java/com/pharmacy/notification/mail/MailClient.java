package com.pharmacy.notification.mail;

import com.pharmacy.notification.entity.EmailNotificationLog;
import com.pharmacy.notification.repository.EmailNotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class MailClient {
    private static final Logger log = LoggerFactory.getLogger(MailClient.class);
    private final EmailNotificationLogRepository logs;
    private final SmtpGateway smtp;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;

    public MailClient(
            EmailNotificationLogRepository logs,
            SmtpGateway smtp,
            @Value("${spring.mail.host:smtp.gmail.com}") String host,
            @Value("${spring.mail.port:587}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${app.mail.from:}") String from) {
        this.logs = logs;
        this.smtp = smtp;
        this.host = host == null ? "smtp.gmail.com" : host.trim();
        this.port = port;
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password.replace(" ", "").trim();
        this.from = from == null || from.isBlank() ? this.username : from.trim();
    }

    public String send(String type, String to, String subject, String body) {
        return send(type, null, List.of(to), subject, body);
    }

    public String send(String type, List<String> recipients, String subject, String body) {
        return send(type, null, recipients, subject, body);
    }

    public String send(String type, String orderNumber, List<String> recipients, String subject, String body) {
        Set<String> to = new LinkedHashSet<>();
        if (recipients != null) {
            for (String address : recipients) {
                if (address != null && !address.isBlank()) {
                    to.add(address.trim());
                }
            }
        }
        if (to.isEmpty()) {
            throw new IllegalArgumentException("No email recipient");
        }
        String joined = String.join(", ", to);
        EmailNotificationLog row = new EmailNotificationLog();
        row.setType(type);
        row.setOrderNumber(orderNumber);
        row.setRecipient(joined);
        row.setSubject(subject);
        row.setBody(body);
        if (orderNumber != null && !orderNumber.isBlank()
                && logs.existsByTypeAndOrderNumberAndStatus(type, orderNumber, "SENT")) {
            log.info("Email skipped duplicate type={} orderNumber={}", type, orderNumber);
            return "Email already sent to " + joined;
        }

        if (username.isBlank() || password.isBlank()) {
            row.setStatus("NOT_CONFIGURED");
            logs.save(row);
            log.warn("Email not sent type={} to={} reason=smtp-not-configured", type, joined);
            throw new IllegalStateException("Email was not delivered. Set spring.mail.username and spring.mail.password.");
        }

        try {
            smtp.send(host, port, username, password, from.isBlank() ? username : from,
                    List.copyOf(to), subject, body);
            row.setStatus("SENT");
            logs.save(row);
            log.info("Email delivered type={} to={} subject={}", type, joined, subject);
            return "Email sent to " + joined;
        } catch (Exception ex) {
            row.setStatus("FAILED");
            logs.save(row);
            log.warn("Email delivery failed type={} to={} reason={}", type, joined, ex.getMessage());
            throw new IllegalStateException("Could not deliver email to " + joined + ": " + ex.getMessage());
        }
    }
}
