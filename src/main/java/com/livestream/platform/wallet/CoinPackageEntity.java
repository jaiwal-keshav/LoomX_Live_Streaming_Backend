package com.livestream.platform.wallet;

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
@Table(name = "coin_packages")
public class CoinPackageEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(nullable = false, length = 100)
    String name;
    @Column(nullable = false)
    long coins;
    @Column(name = "bonus_coins", nullable = false)
    long bonusCoins;
    @Column(name = "price_minor", nullable = false)
    long priceMinor;
    @Column(nullable = false, length = 3)
    String currency;
    @Column(name = "is_active", nullable = false)
    boolean active;
    @Column(name = "display_order", nullable = false)
    int displayOrder;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected CoinPackageEntity() {
    }
}
