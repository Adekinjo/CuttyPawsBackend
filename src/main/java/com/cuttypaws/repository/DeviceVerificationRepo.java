package com.cuttypaws.repository;

import com.cuttypaws.entity.DeviceVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface DeviceVerificationRepo extends JpaRepository<DeviceVerification, Long> {
    Optional<DeviceVerification> findByEmailAndDeviceId(String email, String deviceId);

    void deleteByExpiryDateBefore(LocalDateTime expiryDate);

    void deleteByEmailAndDeviceId(String email, String deviceId);

    Optional<DeviceVerification> findByEmailAndDeviceIdAndExpiryDateAfter(String email, String deviceId, LocalDateTime now);

    Optional<DeviceVerification> findByEmailAndDeviceIdAndVerifiedTrue(String email, String deviceId);
}