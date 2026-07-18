package com.livestream.platform.streaming;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface StreamParticipantRepository extends JpaRepository<StreamParticipantEntity, UUID> {
    Optional<StreamParticipantEntity> findByStreamIdAndUserIdAndLeftAtIsNull(UUID streamId, UUID userId);
    List<StreamParticipantEntity> findByStreamIdAndLeftAtIsNull(UUID streamId);
    boolean existsByStreamIdAndUserIdAndParticipantType(UUID streamId, UUID userId, String participantType);
}
