package com.livestream.platform.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "admin_id", nullable = false)
    UUID adminId;
    @Column(nullable = false, length = 100)
    String action;
    @Column(nullable = false, length = 50)
    String module;
    @Column(name = "entity_type", nullable = false, length = 50)
    String entityType;
    @Column(name = "entity_id", nullable = false, length = 255)
    String entityId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb")
    String oldValue;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb")
    String newValue;
    @Column(name = "ip_address", length = 45)
    String ipAddress;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    protected AuditLogEntity() {
    }
}
