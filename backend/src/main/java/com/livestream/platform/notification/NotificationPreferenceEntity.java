package com.livestream.platform.notification;

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
@Table(name = "notification_preferences")
public class NotificationPreferenceEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;
    @Column(name = "stream_notifications", nullable = false)
    private boolean streamNotifications = true;
    @Column(name = "message_notifications", nullable = false)
    private boolean messageNotifications = true;
    @Column(name = "gift_notifications", nullable = false)
    private boolean giftNotifications = true;
    @Column(name = "follow_notifications", nullable = false)
    private boolean followNotifications = true;
    @Column(name = "call_notifications", nullable = false)
    private boolean callNotifications = true;
    @Column(name = "promotional_notifications", nullable = false)
    private boolean promotionalNotifications;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationPreferenceEntity() {
    }

    public static NotificationPreferenceEntity defaults(UUID userId) {
        NotificationPreferenceEntity preferences = new NotificationPreferenceEntity();
        preferences.userId = userId;
        return preferences;
    }
}
