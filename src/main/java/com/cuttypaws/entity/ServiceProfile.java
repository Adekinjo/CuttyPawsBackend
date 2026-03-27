package com.cuttypaws.entity;

import com.cuttypaws.enums.ServiceStatus;
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
        name = "service_profiles",
        indexes = {
                @Index(name = "idx_service_profile_user_id", columnList = "user_id"),
                @Index(name = "idx_service_profile_status", columnList = "status"),
                @Index(name = "idx_service_profile_city", columnList = "city"),
                @Index(name = "idx_service_profile_state", columnList = "state"),
                @Index(name = "idx_service_profile_service_type", columnList = "service_type")
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "tagline")
    private String tagline;

    @Column(name = "description", length = 3000)
    private String description;

    private String city;
    private String state;
    private String country;
    private String zipcode;

    @Column(name = "service_area")
    private String serviceArea;

    @Column(name = "address_line")
    private String addressLine;

    private Double latitude;
    private Double longitude;

    @Column(name = "price_from", precision = 12, scale = 2)
    private BigDecimal priceFrom;

    @Column(name = "price_to", precision = 12, scale = 2)
    private BigDecimal priceTo;

    @Column(name = "pricing_note")
    private String pricingNote;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "whatsapp_number")
    private String whatsappNumber;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "accepts_home_visits", nullable = false)
    @Builder.Default
    private Boolean acceptsHomeVisits = false;

    @Column(name = "offers_emergency_service", nullable = false)
    @Builder.Default
    private Boolean offersEmergencyService = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.PENDING;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "average_rating", nullable = false)
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Long reviewCount = 0L;

    @OneToMany(mappedBy = "serviceProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<ServiceMedia> media = new java.util.ArrayList<>();

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