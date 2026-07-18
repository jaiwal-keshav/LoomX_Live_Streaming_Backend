package com.livestream.platform.auth;

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
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "token_family_id", nullable = false)
    private UUID tokenFamilyId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RefreshTokenEntity() {
    }

    public static RefreshTokenEntity create(UUID userId, UUID deviceId, String tokenHash,
                                            UUID familyId, Instant expiresAt, String ipAddress,
                                            String userAgent) {
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.userId = userId;
        token.deviceId = deviceId;
        token.tokenHash = tokenHash;
        token.tokenFamilyId = familyId;
        token.expiresAt = expiresAt;
        token.ipAddress = ipAddress;
        token.userAgent = userAgent;
        return token;
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID deviceId() { return deviceId; }
    public UUID tokenFamilyId() { return tokenFamilyId; }

    public boolean active(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke() {
        if (revokedAt == null) {
            revokedAt = Instant.now();
        }
    }
}
