package com.livestream.platform.streaming;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface StreamInvitationRepository extends JpaRepository<StreamInvitationEntity, UUID> {
    Optional<StreamInvitationEntity> findByStreamIdAndInviteeIdAndStatus(UUID streamId, UUID inviteeId, String status);
    List<StreamInvitationEntity> findByInviteeIdAndStatusOrderBySentAtDesc(UUID inviteeId, String status);
    List<StreamInvitationEntity> findByStreamIdAndStatus(UUID streamId, String status);
}
