package com.livestream.platform.auth;

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
@Table(name = "devices")
public class DeviceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(nullable = false, length = 20)
    private String platform = "ANDROID";

    @Column(name = "app_version", length = 30)
    private String appVersion;

    @Column(name = "os_version", length = 30)
    private String osVersion;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DeviceEntity() {
    }

    public static DeviceEntity create(UUID userId, String deviceId, String deviceName,
                                      String appVersion, String osVersion, String fcmToken) {
        DeviceEntity device = new DeviceEntity();
        device.userId = userId;
        device.deviceId = deviceId;
        device.update(deviceName, appVersion, osVersion, fcmToken);
        return device;
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public String deviceId() { return deviceId; }
    public String deviceName() { return deviceName; }
    public String appVersion() { return appVersion; }
    public String osVersion() { return osVersion; }
    public boolean active() { return active; }
    public Instant lastActiveAt() { return lastActiveAt; }

    public void update(String deviceName, String appVersion, String osVersion, String fcmToken) {
        this.deviceName = deviceName;
        this.appVersion = appVersion;
        this.osVersion = osVersion;
        this.fcmToken = fcmToken;
        this.lastActiveAt = Instant.now();
        this.active = true;
    }

    public void deactivate() {
        active = false;
    }
}
