package com.livestream.platform.moderation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stream_moderation_actions")
public class StreamModerationActionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "stream_id", nullable = false)
    UUID streamId;
    @Column(name = "target_user_id", nullable = false)
    UUID targetUserId;
    @Column(name = "actor_user_id", nullable = false)
    UUID actorUserId;
    @Column(nullable = false, length = 20)
    String action;
    String reason;
    @Column(name = "expires_at")
    Instant expiresAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    protected StreamModerationActionEntity() {
    }
}
