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
@Table(name = "gifts")
public class GiftEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "category_id", nullable = false)
    UUID categoryId;
    @Column(nullable = false, length = 100)
    String name;
    String description;
    @Column(name = "icon_url")
    String iconUrl;
    @Column(name = "animation_url")
    String animationUrl;
    @Column(name = "coin_cost", nullable = false)
    long coinCost;
    @Column(name = "diamond_reward", nullable = false)
    long diamondReward;
    @Column(nullable = false, length = 20)
    String rarity;
    @Column(name = "is_limited", nullable = false)
    boolean limited;
    @Column(name = "is_active", nullable = false)
    boolean active;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected GiftEntity() {
    }
}
