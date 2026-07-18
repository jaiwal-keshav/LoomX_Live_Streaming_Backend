package com.livestream.platform.auth;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

    public enum LoginProvider { PHONE, EMAIL, GOOGLE, APPLE, FACEBOOK }
    public enum Status { ACTIVE, BLOCKED, SUSPENDED, DELETED }
    public enum Role { VIEWER, STREAMER }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_provider", nullable = false, length = 20)
    private LoginProvider loginProvider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    protected UserEntity() {
    }

    public static UserEntity phoneUser(String phoneNumber) {
        UserEntity user = new UserEntity();
        user.phoneNumber = phoneNumber;
        user.providerUserId = phoneNumber;
        user.loginProvider = LoginProvider.PHONE;
        user.phoneVerified = true;
        user.roles.add(Role.VIEWER);
        return user;
    }

    public static UserEntity emailUser(String email, String passwordHash) {
        UserEntity user = new UserEntity();
        user.email = email;
        user.providerUserId = email;
        user.passwordHash = passwordHash;
        user.loginProvider = LoginProvider.EMAIL;
        user.emailVerified = true;
        user.roles.add(Role.VIEWER);
        return user;
    }

    public UUID id() { return id; }
    public String phoneNumber() { return phoneNumber; }
    public String email() { return email; }
    public String passwordHash() { return passwordHash; }
    public LoginProvider loginProvider() { return loginProvider; }
    public Status status() { return status; }
    public Set<Role> roles() { return Set.copyOf(roles); }
    public Instant createdAt() { return createdAt; }

    public boolean canAuthenticate() {
        return status == Status.ACTIVE
                && (loginProvider != LoginProvider.EMAIL || emailVerified)
                && (loginProvider != LoginProvider.PHONE || phoneVerified);
    }

    public void recordLogin() {
        lastLoginAt = Instant.now();
    }

    public void changePassword(String newPasswordHash) {
        passwordHash = newPasswordHash;
    }

    public void softDelete() {
        status = Status.DELETED;
        deletedAt = Instant.now();
    }
}
