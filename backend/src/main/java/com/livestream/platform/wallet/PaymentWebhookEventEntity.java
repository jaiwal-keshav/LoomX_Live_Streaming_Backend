package com.livestream.platform.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "payment_webhook_events")
public class PaymentWebhookEventEntity {
    @Id @Column(name = "event_id", length = 100)
    String eventId;
    @Column(name = "event_type", nullable = false, length = 100)
    String eventType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    String payload;
    @CreationTimestamp @Column(name = "received_at", nullable = false, updatable = false)
    Instant receivedAt;
    @Column(name = "processed_at")
    Instant processedAt;
    @Column(name = "processing_error")
    String processingError;

    protected PaymentWebhookEventEntity() {
    }
}
