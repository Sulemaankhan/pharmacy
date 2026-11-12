package com.pharmacy.notification.mail;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class SmtpGateway {
    private static final Logger log = LoggerFactory.getLogger(SmtpGateway.class);
    private final SmtpMailSender smtp = new SmtpMailSender();

    @CircuitBreaker(name = "smtp", fallbackMethod = "sendFallback")
    public void send(
            String host,
            int port,
            String username,
            String password,
            String from,
            List<String> to,
            String subject,
            String body) {
        try {
            smtp.send(host, port, username, password, from, to, subject, body);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex.getMessage() == null ? "SMTP I/O failed" : ex.getMessage(), ex);
        }
    }

    private void sendFallback(
            String host,
            int port,
            String username,
            String password,
            String from,
            List<String> to,
            String subject,
            String body,
            Throwable ex) {
        if (ex instanceof CallNotPermittedException) {
            log.warn("SMTP skipped, circuit open to={}", to);
        } else {
            log.warn("SMTP send failed to={} reason={}", to, root(ex));
        }
        throw new IllegalStateException("Email service is unavailable. Please try again.");
    }

    private static String root(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : current.getClass().getSimpleName() + ": " + message;
    }
}
