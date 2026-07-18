package com.livestream.platform.streaming;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface StreamJoinRequestRepository extends JpaRepository<StreamJoinRequestEntity, UUID> {
    Optional<StreamJoinRequestEntity> findByStreamIdAndRequesterIdAndStatus(UUID streamId, UUID requesterId, String status);
    Optional<StreamJoinRequestEntity> findTopByStreamIdAndRequesterIdOrderByRequestedAtDesc(UUID streamId, UUID requesterId);
    List<StreamJoinRequestEntity> findByStreamIdAndStatusOrderByRequestedAtAsc(UUID streamId, String status);
}
