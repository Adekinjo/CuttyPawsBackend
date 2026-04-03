package com.cuttypaws.dto;

import com.cuttypaws.enums.ServiceType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProfileRequestDto {

    @NotNull
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

    private Boolean acceptsHomeVisits;
    private Boolean offersEmergencyService;
}