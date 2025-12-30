package com.cuttypaws.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_verification")
@Data
public class DeviceVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private String verificationCode;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private int attemptCount = 0;

    @PrePersist
    protected void onCreate() {
        if (expiryDate == null) {
            expiryDate = LocalDateTime.now().plusMinutes(10);
        }
    }

    private LocalDateTime VerifiedAt;

    private LocalDateTime CreatedAt;
}