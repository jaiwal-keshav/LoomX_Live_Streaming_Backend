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
@Table(name = "stream_categories")
public class StreamCategoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(nullable = false, unique = true, length = 100)
    String name;
    @Column(name = "icon_url")
    String iconUrl;
    @Column(name = "banner_url")
    String bannerUrl;
    @Column(name = "display_order", nullable = false)
    int displayOrder;
    @Column(name = "is_active", nullable = false)
    boolean active;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected StreamCategoryEntity() {
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public String iconUrl() { return iconUrl; }
    public String bannerUrl() { return bannerUrl; }
    public int displayOrder() { return displayOrder; }
    public boolean active() { return active; }
}
