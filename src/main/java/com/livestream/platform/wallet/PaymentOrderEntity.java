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
@Table(name = "payment_orders")
public class PaymentOrderEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "user_id", nullable = false)
    UUID userId;
    @Column(name = "product_type", nullable = false, length = 30)
    String productType;
    @Column(name = "product_id", nullable = false)
    UUID productId;
    @Column(name = "grant_currency", nullable = false, length = 30)
    String grantCurrency;
    @Column(name = "grant_amount", nullable = false)
    long grantAmount;
    @Column(name = "amount_minor", nullable = false)
    long amountMinor;
    @Column(nullable = false, length = 3)
    String currency;
    @Column(nullable = false, length = 20)
    String provider;
    @Column(name = "provider_order_id", length = 100)
    String providerOrderId;
    @Column(name = "provider_payment_id", length = 100)
    String providerPaymentId;
    @Column(nullable = false, length = 20)
    String status;
    @Column(name = "idempotency_key", nullable = false, length = 100)
    String idempotencyKey;
    @Column(name = "failure_reason")
    String failureReason;
    @Column(name = "captured_at")
    Instant capturedAt;
    @Column(name = "refunded_at")
    Instant refundedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected PaymentOrderEntity() {
    }
}
