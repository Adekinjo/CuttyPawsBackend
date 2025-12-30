package com.cuttypaws.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_events")
@Data
public class SecurityEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType; // MALICIOUS_LOGIN, XSS_ATTEMPT, etc.
    private String description;
    private String ipAddress;
    private String userEmail;
    private LocalDateTime timestamp = LocalDateTime.now();
    private boolean resolved = false;

    // Location fields
    private String country;
    private String city;
    private String isp;
}