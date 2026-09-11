package com.pharmacy.drugstore.notification;

import com.pharmacy.drugstore.entity.EmailNotificationLog;
import com.pharmacy.drugstore.repository.EmailNotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class MailClient {
    private static final Logger log = LoggerFactory.getLogger(MailClient.class);
    private final EmailNotificationLogRepository logs;
    private final SmtpMailSender smtp = new SmtpMailSender();
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;

    public MailClient(
            EmailNotificationLogRepository logs,
            @Value("${spring.mail.host:smtp.gmail.com}") String host,
            @Value("${spring.mail.port:587}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${app.mail.from:}") String from) {
        this.logs = logs;
        this.host = host == null ? "smtp.gmail.com" : host.trim();
        this.port = port;
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password.replace(" ", "").trim();
        this.from = from == null || from.isBlank() ? this.username : from.trim();
    }

    public String send(String type, String to, String subject, String body) {
        return send(type, List.of(to), subject, body);
    }

    public String send(String type, List<String> recipients, String subject, String body) {
        Set<String> to = new LinkedHashSet<>();
        if (recipients != null) {
            for (String address : recipients) {
                if (address != null && !address.isBlank()) {
                    to.add(address.trim());
                }
            }
        }
        if (to.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No email recipient");
        }
        String joined = String.join(", ", to);
        EmailNotificationLog row = new EmailNotificationLog();
        row.setType(type);
        row.setRecipient(joined);
        row.setSubject(subject);
        row.setBody(body);

        if (username.isBlank() || password.isBlank()) {
            row.setStatus("NOT_CONFIGURED");
            logs.save(row);
            log.warn("Email not sent type={} to={} reason=smtp-not-configured", type, joined);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email was not delivered. Set spring.mail.username and spring.mail.password, then restart.");
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
            log.error("Email delivery failed type={} to={}", type, joined, ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not deliver email to " + joined + ": " + ex.getMessage());
        }
    }
}
