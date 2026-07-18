package com.livestream.platform.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class WalletEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;
    @Column(name = "coin_balance", nullable = false)
    private long coinBalance;
    @Column(name = "diamond_balance", nullable = false)
    private long diamondBalance;
    @Column(name = "subscription_point_balance", nullable = false)
    private long subscriptionPointBalance;
    @Version
    private long version;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WalletEntity() {
    }

    public static WalletEntity empty(UUID userId) {
        WalletEntity wallet = new WalletEntity();
        wallet.userId = userId;
        return wallet;
    }

    public UUID id() { return id; }
    public long coinBalance() { return coinBalance; }
    public long diamondBalance() { return diamondBalance; }
    public long subscriptionPointBalance() { return subscriptionPointBalance; }
}
