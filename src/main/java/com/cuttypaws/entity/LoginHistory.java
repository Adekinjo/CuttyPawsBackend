package com.cuttypaws.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // user who attempted login
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // email entered during login
    @Column(nullable = false, length = 255)
    private String email;

    // SUCCESS or FAILED
    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 100)
    private String ipAddress;

    @Column(length = 1000)
    private String userAgent;

    @Column(length = 255)
    private String deviceInfo;

    @Column(length = 255)
    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}