package com.livestream.platform.moderation;

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
@Table(name = "reports")
public class ReportEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "reporter_id", nullable = false)
    UUID reporterId;
    @Column(name = "target_type", nullable = false, length = 20)
    String targetType;
    @Column(name = "target_id", nullable = false, length = 255)
    String targetId;
    @Column(nullable = false, length = 30)
    String reason;
    String description;
    @Column(nullable = false, length = 20)
    String status;
    @Column(name = "assigned_admin_id")
    UUID assignedAdminId;
    @Column(name = "resolution_notes")
    String resolutionNotes;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected ReportEntity() {
    }
}
