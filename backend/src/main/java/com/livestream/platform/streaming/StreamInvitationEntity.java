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
@Table(name = "stream_invitations")
public class StreamInvitationEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "stream_id", nullable = false)
    UUID streamId;
    @Column(name = "inviter_id", nullable = false)
    UUID inviterId;
    @Column(name = "invitee_id", nullable = false)
    UUID inviteeId;
    @Column(nullable = false, length = 20)
    String status;
    @Column(name = "sent_at", nullable = false)
    Instant sentAt;
    @Column(name = "responded_at")
    Instant respondedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected StreamInvitationEntity() {
    }

    public static StreamInvitationEntity pending(UUID streamId, UUID inviterId, UUID inviteeId, Instant now) {
        StreamInvitationEntity invitation = new StreamInvitationEntity();
        invitation.streamId = streamId;
        invitation.inviterId = inviterId;
        invitation.inviteeId = inviteeId;
        invitation.status = "PENDING";
        invitation.sentAt = now;
        return invitation;
    }

    public UUID id() { return id; }
    public UUID streamId() { return streamId; }
    public UUID inviterId() { return inviterId; }
    public UUID inviteeId() { return inviteeId; }
    public String status() { return status; }
    public Instant sentAt() { return sentAt; }
    public Instant respondedAt() { return respondedAt; }
    public void respond(String decision, Instant now) {
        status = decision;
        respondedAt = now;
    }
    public void expire(Instant now) { respond("EXPIRED", now); }
}
