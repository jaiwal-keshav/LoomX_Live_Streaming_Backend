package com.livestream.platform.streaming;

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
@Table(name = "stream_likes")
public class StreamLikeEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "stream_id", nullable = false)
    UUID streamId;
    @Column(name = "user_id", nullable = false)
    UUID userId;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    protected StreamLikeEntity() {
    }

    public static StreamLikeEntity create(UUID streamId, UUID userId) {
        StreamLikeEntity like = new StreamLikeEntity();
        like.streamId = streamId;
        like.userId = userId;
        return like;
    }

    public UUID id() { return id; }
}
