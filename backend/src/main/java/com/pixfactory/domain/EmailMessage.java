package com.pixfactory.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "email_messages")
public class EmailMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String recipient;
    @Column(nullable = false)
    private String subject;
    @Column(length = 8000)
    private String body;
    private String status = "SENT";
    private Instant createdAt = Instant.now();
    private String provider = "DEMO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
