package com.cuttypaws.repository;

import com.cuttypaws.entity.ServiceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceReviewRepo extends JpaRepository<ServiceReview, UUID> {

    Optional<ServiceReview> findByServiceProfileIdAndReviewerId(UUID serviceProfileId, UUID reviewerId);

    List<ServiceReview> findByServiceProfileIdOrderByCreatedAtDesc(UUID serviceProfileId);

    @Query("select avg(sr.rating) from ServiceReview sr where sr.serviceProfile.id = :serviceProfileId")
    Double calculateAverageRating(UUID serviceProfileId);

    Long countByServiceProfileId(UUID serviceProfileId);

}