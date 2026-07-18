package com.livestream.platform.gift;

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
@Table(name = "gift_transactions")
public class GiftTransactionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "sender_id", nullable = false)
    UUID senderId;
    @Column(name = "receiver_id", nullable = false)
    UUID receiverId;
    @Column(name = "stream_id", nullable = false)
    UUID streamId;
    @Column(name = "gift_id", nullable = false)
    UUID giftId;
    @Column(nullable = false)
    int quantity;
    @Column(name = "total_coin_cost", nullable = false)
    long totalCoinCost;
    @Column(name = "total_diamond_reward", nullable = false)
    long totalDiamondReward;
    @Column(name = "wallet_transaction_debit_id", nullable = false)
    UUID walletTransactionDebitId;
    @Column(name = "wallet_transaction_credit_id", nullable = false)
    UUID walletTransactionCreditId;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    protected GiftTransactionEntity() {
    }
}
