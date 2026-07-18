package com.livestream.platform.subscription;

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
@Table(name = "subscription_plans")
public class SubscriptionPlanEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(nullable = false, length = 100)
    String name;
    String description;
    @Column(name = "price_minor", nullable = false)
    long priceMinor;
    @Column(nullable = false, length = 3)
    String currency;
    @Column(name = "total_points", nullable = false)
    long totalPoints;
    @Column(name = "validity_days")
    Integer validityDays;
    @Column(name = "is_active", nullable = false)
    boolean active;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected SubscriptionPlanEntity() {
    }
}
