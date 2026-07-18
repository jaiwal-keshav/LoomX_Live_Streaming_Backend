package com.livestream.platform.streaming;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface StreamLikeRepository extends JpaRepository<StreamLikeEntity, UUID> {
    boolean existsByStreamIdAndUserId(UUID streamId, UUID userId);
    Optional<StreamLikeEntity> findByStreamIdAndUserId(UUID streamId, UUID userId);
}
