package com.livestream.platform.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID> {
    Optional<DeviceEntity> findByUserIdAndDeviceId(UUID userId, String deviceId);
    List<DeviceEntity> findByUserIdAndActiveTrueOrderByLastActiveAtDesc(UUID userId);
}
