package com.livestream.platform.streaming;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stream_participants")
public class StreamParticipantEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "stream_id", nullable = false)
    UUID streamId;
    @Column(name = "user_id", nullable = false)
    UUID userId;
    @Column(name = "participant_type", nullable = false, length = 20)
    String participantType;
    @Column(name = "joined_at", nullable = false)
    Instant joinedAt;
    @Column(name = "left_at")
    Instant leftAt;

    protected StreamParticipantEntity() {
    }

    public static StreamParticipantEntity join(UUID streamId, UUID userId, String type, Instant now) {
        StreamParticipantEntity participant = new StreamParticipantEntity();
        participant.streamId = streamId;
        participant.userId = userId;
        participant.participantType = type;
        participant.joinedAt = now;
        return participant;
    }

    public UUID id() { return id; }
    public UUID streamId() { return streamId; }
    public UUID userId() { return userId; }
    public String participantType() { return participantType; }
    public Instant joinedAt() { return joinedAt; }
    public boolean active() { return leftAt == null; }
    public void leave(Instant now) { leftAt = now; }
}
