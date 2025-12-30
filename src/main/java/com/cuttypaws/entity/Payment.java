package com.cuttypaws.entity;

import com.cuttypaws.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionId;
    private String email;
    private String reference;

    private BigDecimal amount;

    @Column(name = "method", nullable = false)
    private String method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = true) // CHANGED: nullable = true
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private  final LocalDateTime createdAt = LocalDateTime.now();
}
