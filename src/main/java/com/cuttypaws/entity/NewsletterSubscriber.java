package com.cuttypaws.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "newsletter_subscribers", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email") // Ensure email is unique
})
public class NewsletterSubscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isActive = true;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean subscribed = true;

//    @Column(nullable = false, columnDefinition = "BIT DEFAULT 1")
//    private Boolean isActive = true; // Indicates whether the user is subscribed
//
//    @Column(nullable = false, columnDefinition = "BIT DEFAULT 1")
//    private Boolean subscribed = true; // Indicates whether the user has ever subscribed

    @Column(name = "created_at", nullable = false, updatable = false)
    private final LocalDateTime createdAt = LocalDateTime.now();
}