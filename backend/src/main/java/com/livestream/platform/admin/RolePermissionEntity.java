package com.livestream.platform.admin;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "role_permissions")
public class RolePermissionEntity {
    @EmbeddedId
    RolePermissionId id;

    protected RolePermissionEntity() {
    }
}
