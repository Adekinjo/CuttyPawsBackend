package com.cuttypaws.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerSupportDto {
    private Long id;
    private String customerId; // Add this field
    private String customerName; // Add this field
    private String email;
    private String subject;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    private boolean resolved;

    // Getters and Setters
}