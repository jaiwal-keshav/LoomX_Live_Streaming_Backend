package com.livestream.platform.notification;

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
@Table(name = "notifications")
public class NotificationEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "user_id", nullable = false)
    UUID userId;
    @Column(nullable = false, length = 50)
    String type;
    @Column(nullable = false, length = 200)
    String title;
    @Column(nullable = false)
    String body;
    @Column(name = "image_url")
    String imageUrl;
    @Column(name = "deep_link", length = 500)
    String deepLink;
    @Column(name = "reference_type", length = 30)
    String referenceType;
    @Column(name = "reference_id", length = 255)
    String referenceId;
    @Column(name = "is_read", nullable = false)
    boolean read;
    @Column(name = "read_at")
    Instant readAt;
    @Column(name = "delivery_status", nullable = false, length = 20)
    String deliveryStatus;
    @Column(name = "attempt_count", nullable = false)
    int attemptCount;
    @Column(name = "last_attempt_at")
    Instant lastAttemptAt;
    @Column(name = "last_error")
    String lastError;
    @Column(name = "sent_at")
    Instant sentAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    protected NotificationEntity() {
    }
}
