package com.livestream.platform.streaming;

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
@Table(name = "stream_join_requests")
public class StreamJoinRequestEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "stream_id", nullable = false)
    UUID streamId;
    @Column(name = "requester_id", nullable = false)
    UUID requesterId;
    @Column(nullable = false, length = 20)
    String status;
    @Column(name = "requested_at", nullable = false)
    Instant requestedAt;
    @Column(name = "responded_at")
    Instant respondedAt;
    @Column(name = "responded_by")
    UUID respondedBy;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected StreamJoinRequestEntity() {
    }

    public static StreamJoinRequestEntity pending(UUID streamId, UUID requesterId, Instant now) {
        StreamJoinRequestEntity request = new StreamJoinRequestEntity();
        request.streamId = streamId;
        request.requesterId = requesterId;
        request.status = "PENDING";
        request.requestedAt = now;
        return request;
    }

    public UUID id() { return id; }
    public UUID streamId() { return streamId; }
    public UUID requesterId() { return requesterId; }
    public String status() { return status; }
    public Instant requestedAt() { return requestedAt; }
    public Instant respondedAt() { return respondedAt; }
    public void respond(String decision, UUID responderId, Instant now) {
        status = decision;
        respondedBy = responderId;
        respondedAt = now;
    }
    public void expire(Instant now) { respond("EXPIRED", null, now); }
}
