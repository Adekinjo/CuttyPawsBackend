// Deal.java
package com.cuttypaws.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "deals")
@Data
public class Deal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    private boolean active = true;

    @Column(name = "created_at")
    private final LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void validateDates() {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }
}