package com.livestream.platform.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    private String bio;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(length = 30)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @Column(name = "is_private", nullable = false)
    private boolean privateProfile;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfileEntity() {
    }

    public static UserProfileEntity create(UUID userId, String username, String displayName) {
        UserProfileEntity profile = new UserProfileEntity();
        profile.userId = userId;
        profile.username = username;
        profile.displayName = displayName;
        return profile;
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public String username() { return username; }
    public String displayName() { return displayName; }
    public String bio() { return bio; }
    public String avatarUrl() { return avatarUrl; }
    public String coverImageUrl() { return coverImageUrl; }
    public String gender() { return gender; }
    public LocalDate dateOfBirth() { return dateOfBirth; }
    public String countryCode() { return countryCode; }
    public String languageCode() { return languageCode; }
    public boolean privateProfile() { return privateProfile; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    public void update(String username, String displayName, String bio, String avatarUrl,
                       String coverImageUrl, String gender, LocalDate dateOfBirth,
                       String countryCode, String languageCode, boolean privateProfile) {
        this.username = username;
        this.displayName = displayName;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.coverImageUrl = coverImageUrl;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.countryCode = countryCode;
        this.languageCode = languageCode;
        this.privateProfile = privateProfile;
    }
}
