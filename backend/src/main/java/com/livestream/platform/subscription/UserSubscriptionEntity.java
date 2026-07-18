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
@Table(name = "user_subscriptions")
public class UserSubscriptionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "user_id", nullable = false)
    UUID userId;
    @Column(name = "plan_id", nullable = false)
    UUID planId;
    @Column(name = "payment_order_id")
    UUID paymentOrderId;
    @Column(name = "total_points", nullable = false)
    long totalPoints;
    @Column(nullable = false, length = 20)
    String status;
    @Column(name = "purchased_at", nullable = false)
    Instant purchasedAt;
    @Column(name = "expires_at")
    Instant expiresAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected UserSubscriptionEntity() {
    }
}
