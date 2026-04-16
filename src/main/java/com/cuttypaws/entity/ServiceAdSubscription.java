package com.cuttypaws.entity;

import com.cuttypaws.enums.AdPlanType;
import com.cuttypaws.enums.PaymentProvider;
import com.cuttypaws.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "service_ad_subscriptions",
        indexes = {
                @Index(name = "idx_service_ad_subscription_service_profile_id", columnList = "service_profile_id"),
                @Index(name = "idx_service_ad_subscription_payment_status", columnList = "payment_status"),
                @Index(name = "idx_service_ad_subscription_is_active", columnList = "is_active"),
                @Index(name = "idx_service_ad_subscription_payment_reference", columnList = "payment_reference"),
                @Index(name = "idx_service_ad_subscription_checkout_session_id", columnList = "checkout_session_id")
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceAdSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_profile_id", nullable = false)
    private ServiceProfile serviceProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private AdPlanType planType;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider", nullable = false)
    private PaymentProvider paymentProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_reference", unique = true, nullable = false)
    private String paymentReference;

    @Column(name = "checkout_session_id", unique = true)
    private String checkoutSessionId;

    @Column(name = "payment_intent_id")
    private String paymentIntentId;

    @Column(name = "payment_url", length = 2000)
    private String paymentUrl;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}