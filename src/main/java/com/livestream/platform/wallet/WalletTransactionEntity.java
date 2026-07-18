package com.livestream.platform.wallet;

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
@Table(name = "wallet_transactions")
public class WalletTransactionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "wallet_id", nullable = false)
    UUID walletId;
    @Column(nullable = false, length = 30)
    String currency;
    @Column(nullable = false, length = 10)
    String type;
    @Column(nullable = false)
    long amount;
    @Column(nullable = false, length = 30)
    String source;
    @Column(name = "reference_type", length = 50)
    String referenceType;
    @Column(name = "reference_id")
    UUID referenceId;
    @Column(name = "balance_before", nullable = false)
    long balanceBefore;
    @Column(name = "balance_after", nullable = false)
    long balanceAfter;
    @Column(name = "idempotency_key", length = 100)
    String idempotencyKey;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    protected WalletTransactionEntity() {
    }
}
