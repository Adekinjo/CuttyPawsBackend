package com.cuttypaws.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class CustomerSupport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerId; // Add this field
    private String customerName; // Add this field
    private String subject;
    private String message;
    private String email;
    private boolean resolved;
    private String status; // Open, In Progress, Closed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters
}