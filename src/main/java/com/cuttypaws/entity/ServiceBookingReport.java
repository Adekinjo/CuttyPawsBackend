package com.cuttypaws.entity;

import com.cuttypaws.enums.BookingReportReason;
import com.cuttypaws.enums.BookingReportStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "service_booking_reports",
        indexes = {
                @Index(name = "idx_booking_report_booking_id", columnList = "booking_id"),
                @Index(name = "idx_booking_report_customer_id", columnList = "customer_id"),
                @Index(name = "idx_booking_report_provider_user_id", columnList = "provider_user_id"),
                @Index(name = "idx_booking_report_status", columnList = "status"),
                @Index(name = "idx_booking_report_created_at", columnList = "created_at")
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBookingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private ServiceBooking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_user_id", nullable = false)
    private User providerUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private BookingReportReason reason;

    @Column(name = "details", length = 3000, nullable = false)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private BookingReportStatus status = BookingReportStatus.OPEN;

    @Column(name = "admin_note", length = 3000)
    private String adminNote;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

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