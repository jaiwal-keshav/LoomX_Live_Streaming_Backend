package com.livestream.platform.call;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "call_sessions")
public class CallSessionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "call_request_id")
    UUID callRequestId;
    @Column(name = "room_id", nullable = false, unique = true, length = 255)
    String roomId;
    @Column(name = "caller_id", nullable = false)
    UUID callerId;
    @Column(name = "receiver_id", nullable = false)
    UUID receiverId;
    @Column(name = "call_type", nullable = false, length = 10)
    String callType;
    @Column(nullable = false, length = 20)
    String status;
    @Column(name = "subscription_id")
    UUID subscriptionId;
    @Column(name = "started_at", nullable = false)
    Instant startedAt;
    @Column(name = "connected_at")
    Instant connectedAt;
    @Column(name = "ended_at")
    Instant endedAt;
    @Column(name = "duration_seconds", nullable = false)
    int durationSeconds;
    @Column(name = "points_consumed", nullable = false)
    long pointsConsumed;
    @Column(name = "end_reason", length = 30)
    String endReason;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected CallSessionEntity() {
    }
}
