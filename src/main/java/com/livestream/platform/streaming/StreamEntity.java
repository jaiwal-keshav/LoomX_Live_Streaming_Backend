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
@Table(name = "streams")
public class StreamEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "room_id", nullable = false, unique = true, length = 255)
    String roomId;
    @Column(name = "host_id", nullable = false)
    UUID hostId;
    @Column(name = "category_id")
    UUID categoryId;
    @Column(nullable = false, length = 200)
    String title;
    String description;
    @Column(name = "thumbnail_url")
    String thumbnailUrl;
    @Column(name = "stream_type", nullable = false, length = 20)
    String streamType;
    @Column(nullable = false, length = 20)
    String status;
    @Column(name = "max_broadcasters", nullable = false)
    int maxBroadcasters;
    @Column(name = "current_viewer_count", nullable = false)
    long currentViewerCount;
    @Column(name = "total_gift_count", nullable = false)
    long totalGiftCount;
    @Column(name = "total_gift_coin_value", nullable = false)
    long totalGiftCoinValue;
    @Column(name = "total_like_count", nullable = false)
    long totalLikeCount;
    @Column(name = "total_watch_seconds", nullable = false)
    long totalWatchSeconds;
    @Column(name = "total_unique_viewers", nullable = false)
    long totalUniqueViewers;
    @Column(name = "started_at")
    Instant startedAt;
    @Column(name = "ended_at")
    Instant endedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected StreamEntity() {
    }

    public static StreamEntity start(String roomId, UUID hostId, UUID categoryId, String title,
                                     String description, String thumbnailUrl, Instant now) {
        StreamEntity stream = new StreamEntity();
        stream.roomId = roomId;
        stream.hostId = hostId;
        stream.categoryId = categoryId;
        stream.title = title;
        stream.description = description;
        stream.thumbnailUrl = thumbnailUrl;
        stream.streamType = "PUBLIC";
        stream.status = "LIVE";
        stream.maxBroadcasters = 5;
        stream.startedAt = now;
        return stream;
    }

    public UUID id() { return id; }
    public String roomId() { return roomId; }
    public UUID hostId() { return hostId; }
    public UUID categoryId() { return categoryId; }
    public String title() { return title; }
    public String description() { return description; }
    public String thumbnailUrl() { return thumbnailUrl; }
    public String status() { return status; }
    public int maxBroadcasters() { return maxBroadcasters; }
    public long currentViewerCount() { return currentViewerCount; }
    public long totalLikeCount() { return totalLikeCount; }
    public long totalWatchSeconds() { return totalWatchSeconds; }
    public long totalUniqueViewers() { return totalUniqueViewers; }
    public Instant startedAt() { return startedAt; }
    public Instant endedAt() { return endedAt; }
    public boolean live() { return "LIVE".equals(status); }

    public void viewerJoined(boolean firstVisit) {
        currentViewerCount++;
        if (firstVisit) totalUniqueViewers++;
    }

    public void viewerLeft(long watchSeconds) {
        currentViewerCount = Math.max(0, currentViewerCount - 1);
        totalWatchSeconds += Math.max(0, watchSeconds);
    }

    public void likeAdded() { totalLikeCount++; }
    public void likeRemoved() { totalLikeCount = Math.max(0, totalLikeCount - 1); }

    public void end(Instant now) {
        status = "ENDED";
        endedAt = now;
        currentViewerCount = 0;
    }
}
