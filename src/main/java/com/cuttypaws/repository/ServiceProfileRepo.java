package com.cuttypaws.repository;

import com.cuttypaws.entity.ServiceProfile;
import com.cuttypaws.enums.ServiceStatus;
import com.cuttypaws.enums.ServiceType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceProfileRepo extends JpaRepository<ServiceProfile, UUID> {

    @EntityGraph(attributePaths = {"user"})
    Optional<ServiceProfile> findByUserId(UUID userId);


    @EntityGraph(attributePaths = {"user"})
    List<ServiceProfile> findByStatusOrderByCreatedAtAsc(ServiceStatus status);


    List<ServiceProfile> findByStatusAndCityIgnoreCaseAndStateIgnoreCase(
            ServiceStatus status,
            String city,
            String state,
            Pageable pageable
    );

    List<ServiceProfile> findByStatusAndServiceTypeAndCityIgnoreCaseAndStateIgnoreCase(
            ServiceStatus status,
            ServiceType serviceType,
            String city,
            String state,
            Pageable pageable
    );

    List<ServiceProfile> findByStatusAndServiceTypeOrderByCreatedAtDesc(
            ServiceStatus status,
            ServiceType serviceType,
            Pageable pageable
    );

    List<ServiceProfile> findByStatusOrderByCreatedAtDesc(
            ServiceStatus status,
            Pageable pageable
    );

    List<ServiceProfile> findTop8ByStatusAndBusinessNameContainingIgnoreCaseOrderByCreatedAtDesc(
            ServiceStatus status,
            String businessName
    );

    List<ServiceProfile> findTop8ByStatusAndDescriptionContainingIgnoreCaseOrderByCreatedAtDesc(
            ServiceStatus status,
            String description
    );

    List<ServiceProfile> findTop8ByStatusAndCityContainingIgnoreCaseOrderByCreatedAtDesc(
            ServiceStatus status,
            String city
    );
}