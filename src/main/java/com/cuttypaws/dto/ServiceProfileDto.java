package com.cuttypaws.dto;

import com.cuttypaws.enums.ServiceStatus;
import com.cuttypaws.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProfileDto {
    private UUID id;
    private UUID userId;
    private String ownerName;
    private String ownerProfileImageUrl;

    private ServiceType serviceType;
    private String businessName;
    private String tagline;
    private String description;

    private String city;
    private String state;
    private String country;
    private String zipcode;
    private String serviceArea;
    private String addressLine;

    private Double latitude;
    private Double longitude;

    private BigDecimal priceFrom;
    private BigDecimal priceTo;
    private String pricingNote;

    private Integer yearsOfExperience;
    private String licenseNumber;
    private String websiteUrl;
    private String whatsappNumber;

    private Boolean isVerified;
    private Boolean acceptsHomeVisits;
    private Boolean offersEmergencyService;
    private ServiceStatus status;
    private String rejectionReason;

    private Double averageRating;
    private Long reviewCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime approvedAt;

    private String displayLabel;

    // public ad/profile fields
    private Boolean sponsored;
    private String sponsoredPlanType;
    private LocalDateTime sponsoredUntil;

    private java.util.List<ServiceMediaDto> media;
    private String coverImageUrl;
}