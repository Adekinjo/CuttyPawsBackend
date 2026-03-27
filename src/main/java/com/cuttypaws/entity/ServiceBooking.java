package com.cuttypaws.entity;

import com.cuttypaws.enums.BookingStatus;
import com.cuttypaws.enums.PaymentStatus;
import com.cuttypaws.enums.ServiceType;
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
        name = "service_bookings",
        indexes = {
                @Index(name = "idx_service_booking_customer_id", columnList = "customer_id"),
                @Index(name = "idx_service_booking_service_profile_id", columnList = "service_profile_id"),
                @Index(name = "idx_service_booking_status", columnList = "booking_status"),
                @Index(name = "idx_service_booking_payment_status", columnList = "payment_status"),
                @Index(name = "idx_service_booking_starts_at", columnList = "starts_at")
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_profile_id", nullable = false)
    private ServiceProfile serviceProfile;

    @Column(name = "pet_name")
    private String petName;

    @Column(name = "pet_type")
    private String petType;

    @Column(name = "service_address", length = 500)
    private String serviceAddress;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "timezone", nullable = false)
    @Builder.Default
    private String timezone = "America/Chicago";

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false)
    @Builder.Default
    private BookingStatus bookingStatus = BookingStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_reference", unique = true)
    private String paymentReference;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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