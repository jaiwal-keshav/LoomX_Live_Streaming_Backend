package com.livestream.platform.call;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "call_requests")
public class CallRequestEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "caller_id", nullable = false)
    UUID callerId;
    @Column(name = "receiver_id", nullable = false)
    UUID receiverId;
    @Column(nullable = false, length = 20)
    String status;
    @Column(name = "requested_at", nullable = false)
    Instant requestedAt;
    @Column(name = "responded_at")
    Instant respondedAt;

    protected CallRequestEntity() {
    }
}
