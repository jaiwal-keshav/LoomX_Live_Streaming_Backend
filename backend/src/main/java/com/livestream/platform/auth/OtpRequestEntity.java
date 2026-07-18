package com.livestream.platform.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_requests")
public class OtpRequestEntity {

    public enum Channel { SMS, EMAIL }
    public enum Purpose { LOGIN, REGISTER, RESET_PASSWORD }

    @Id
    private UUID id;

    @Column(nullable = false, length = 255)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Channel channel;

    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Purpose purpose;

    @Column(name = "attempt_count", nullable = false)
    private short attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private short maxAttempts;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OtpRequestEntity() {
    }

    public static OtpRequestEntity create(String destination, Channel channel, Purpose purpose,
                                          String otpHash, int maxAttempts, Instant expiresAt, String ipAddress) {
        OtpRequestEntity request = new OtpRequestEntity();
        request.id = UUID.randomUUID();
        request.destination = destination;
        request.channel = channel;
        request.purpose = purpose;
        request.otpHash = otpHash;
        request.maxAttempts = (short) maxAttempts;
        request.expiresAt = expiresAt;
        request.ipAddress = ipAddress;
        return request;
    }

    public UUID id() { return id; }
    public String destination() { return destination; }
    public Channel channel() { return channel; }
    public Purpose purpose() { return purpose; }
    public String otpHash() { return otpHash; }
    public Instant expiresAt() { return expiresAt; }
    public short attemptCount() { return attemptCount; }
    public Instant createdAt() { return createdAt; }

    void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public boolean isUsable(Instant now) {
        return verifiedAt == null && invalidatedAt == null && expiresAt.isAfter(now) && attemptCount < maxAttempts;
    }

    public void failedAttempt() {
        if (attemptCount < maxAttempts) {
            attemptCount++;
        }
        if (attemptCount >= maxAttempts) {
            invalidatedAt = Instant.now();
        }
    }

    public void verify() {
        verifiedAt = Instant.now();
        invalidatedAt = verifiedAt;
    }
}
