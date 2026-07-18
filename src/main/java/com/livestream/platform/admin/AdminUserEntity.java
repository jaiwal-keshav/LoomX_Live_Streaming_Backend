package com.livestream.platform.admin;

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
@Table(name = "admin_users")
public class AdminUserEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "first_name", nullable = false, length = 100)
    String firstName;
    @Column(name = "last_name", nullable = false, length = 100)
    String lastName;
    @Column(nullable = false, length = 255)
    String email;
    @Column(name = "password_hash", nullable = false, length = 255)
    String passwordHash;
    @Column(name = "role_id", nullable = false)
    UUID roleId;
    @Column(nullable = false, length = 20)
    String status;
    @Column(name = "last_login_at")
    Instant lastLoginAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected AdminUserEntity() {
    }
}
