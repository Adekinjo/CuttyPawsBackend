package com.cuttypaws.mapper;

import com.cuttypaws.dto.ServiceMediaDto;
import com.cuttypaws.dto.ServiceProfileDto;
import com.cuttypaws.entity.ServiceProfile;
import com.cuttypaws.enums.ServiceType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceProviderMapper {

    public ServiceProfileDto toDto(ServiceProfile profile) {
        return toDto(profile, List.of());
    }

    public ServiceProfileDto toDto(ServiceProfile profile, List<ServiceMediaDto> media) {
        if (profile == null) {
            return null;
        }

        String coverImageUrl = media.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsCover()))
                .map(ServiceMediaDto::getMediaUrl)
                .findFirst()
                .orElse(media.isEmpty() ? null : media.get(0).getMediaUrl());

        return ServiceProfileDto.builder()
                .id(profile.getId())
                .userId(profile.getUser() != null ? profile.getUser().getId() : null)
                .ownerName(profile.getUser() != null ? profile.getUser().getName() : null)
                .ownerProfileImageUrl(profile.getUser() != null ? profile.getUser().getProfileImageUrl() : null)
                .serviceType(profile.getServiceType())
                .businessName(profile.getBusinessName())
                .tagline(profile.getTagline())
                .description(profile.getDescription())
                .city(profile.getCity())
                .state(profile.getState())
                .country(profile.getCountry())
                .zipcode(profile.getZipcode())
                .serviceArea(profile.getServiceArea())
                .addressLine(profile.getAddressLine())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .priceFrom(profile.getPriceFrom())
                .priceTo(profile.getPriceTo())
                .pricingNote(profile.getPricingNote())
                .yearsOfExperience(profile.getYearsOfExperience())
                .licenseNumber(profile.getLicenseNumber())
                .websiteUrl(profile.getWebsiteUrl())
                .whatsappNumber(profile.getWhatsappNumber())
                .isVerified(profile.getIsVerified())
                .acceptsHomeVisits(profile.getAcceptsHomeVisits())
                .offersEmergencyService(profile.getOffersEmergencyService())
                .status(profile.getStatus())
                .rejectionReason(profile.getRejectionReason())
                .averageRating(profile.getAverageRating())
                .reviewCount(profile.getReviewCount())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .reviewedAt(profile.getReviewedAt())
                .approvedAt(profile.getApprovedAt())
                .displayLabel(buildDisplayLabel(profile))
                .sponsored(false)
                .media(media)
                .coverImageUrl(coverImageUrl)
                .build();
    }

    private String buildDisplayLabel(ServiceProfile profile) {
        String typeLabel = mapServiceType(profile.getServiceType());

        String city = profile.getCity();
        String state = profile.getState();

        if (city != null && !city.isBlank() && state != null && !state.isBlank()) {
            return typeLabel + " • " + city + ", " + state;
        }

        if (city != null && !city.isBlank()) {
            return typeLabel + " • " + city;
        }

        return typeLabel;
    }

    private String mapServiceType(ServiceType type) {
        if (type == null) return "Service Provider";

        return switch (type) {
            case PET_WALKER -> "Pet Walker";
            case VETERINARIAN -> "Veterinarian";
            case PET_HOSPITAL -> "Pet Hospital";
            case PET_DAYCARE -> "Pet Daycare";
            case PET_TRAINER -> "Pet Trainer";
            case PET_SELLER -> "Pet Seller";
            case ADOPTION_CENTER -> "Adoption Center";
            case GROOMER -> "Groomer";
            case PET_BOARDING -> "Pet Boarding";
            case PET_SITTER -> "Pet Sitter";
            case BREEDER -> "Breeder";
            case RESCUE_SHELTER -> "Rescue Shelter";
        };
    }
}
