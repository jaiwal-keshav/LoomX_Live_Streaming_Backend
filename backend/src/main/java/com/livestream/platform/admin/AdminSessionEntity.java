package com.livestream.platform.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_sessions")
public class AdminSessionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "admin_id", nullable = false)
    UUID adminId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    String tokenHash;
    @Column(name = "ip_address", length = 45)
    String ipAddress;
    String device;
    @Column(name = "login_at", nullable = false)
    Instant loginAt;
    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;
    @Column(name = "revoked_at")
    Instant revokedAt;

    protected AdminSessionEntity() {
    }
}
