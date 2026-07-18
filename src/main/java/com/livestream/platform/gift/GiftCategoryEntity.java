package com.livestream.platform.gift;

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
@Table(name = "gift_categories")
public class GiftCategoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(nullable = false, length = 100)
    String name;
    String description;
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

    protected GiftCategoryEntity() {
    }
}
