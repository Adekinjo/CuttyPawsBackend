package com.cuttypaws.repository;

import com.cuttypaws.entity.ServiceMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceMediaRepo extends JpaRepository<ServiceMedia, UUID> {
    List<ServiceMedia> findByServiceProfileIdInOrderByDisplayOrderAsc(List<UUID> serviceProfileIds);

    List<ServiceMedia> findByServiceProfileIdOrderByDisplayOrderAsc(UUID serviceProfileId);

    Optional<ServiceMedia> findByIdAndServiceProfileUserId(UUID mediaId, UUID userId);

}