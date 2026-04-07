package com.cuttypaws.entity;

import com.cuttypaws.enums.PaymentProvider;
import com.cuttypaws.enums.PaymentPurpose;
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

    @Column(name = "transaction_id")
    private String transactionId;

    private String email;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private PaymentProvider provider;

    @Column(name = "method", nullable = false)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_purpose", nullable = false)
    private PaymentPurpose paymentPurpose;

    @Column(nullable = false)
    private String currency;

    @Column(name = "payment_intent_id", unique = true)
    private String paymentIntentId;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_ad_subscription_id")
    private ServiceAdSubscription serviceAdSubscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_booking_id")
    private ServiceBooking serviceBooking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}