package com.livestream.platform.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "admin_permissions")
public class AdminPermissionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "permission_key", nullable = false, unique = true, length = 100)
    String permissionKey;
    String description;
    @Column(nullable = false, length = 50)
    String module;

    protected AdminPermissionEntity() {
    }
}
