package com.cuttypaws.repository;

import com.cuttypaws.entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityEventRepo extends JpaRepository<SecurityEvent, Long> {
    List<SecurityEvent> findByResolvedFalseOrderByTimestampDesc();
    List<SecurityEvent> findByIpAddressOrderByTimestampDesc(String ipAddress);
}