package com.pharmacy.drugstore.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "email_notifications")
public class EmailNotificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recipient;
    private String subject;
    @Column(columnDefinition = "TEXT")
    private String body;
    private String type;
    private String status;
    private Instant sentAt = Instant.now();

    public Long getId() { return id; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getSentAt() { return sentAt; }
}
