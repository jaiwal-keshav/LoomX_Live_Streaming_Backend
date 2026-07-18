package com.livestream.platform.user;

import com.livestream.platform.auth.UserEntity;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public final class ProfileModels {
    private ProfileModels() {
    }

    public record UpdateProfileInput(
            @Pattern(regexp = "[A-Za-z0-9_]{3,30}") String username,
            @Size(min = 1, max = 100) String displayName,
            @Size(max = 1000) String bio,
            String avatarUrl,
            String coverImageUrl,
            @Size(max = 30) String gender,
            LocalDate dateOfBirth,
            @Pattern(regexp = "[A-Za-z]{2}") String countryCode,
            @Pattern(regexp = "[A-Za-z0-9-]{2,10}") String languageCode,
            Boolean privateProfile
    ) {
    }

    public record ProfileView(
            UUID userId,
            String username,
            String displayName,
            String bio,
            String avatarUrl,
            String coverImageUrl,
            String gender,
            LocalDate dateOfBirth,
            String countryCode,
            String languageCode,
            boolean privateProfile
    ) {
    }

    public record WalletView(long coins, long diamonds, long subscriptionPoints) {
    }

    public record AccountView(
            UUID id,
            String phoneNumber,
            String email,
            UserEntity.LoginProvider loginProvider,
            UserEntity.Status status,
            Set<UserEntity.Role> roles,
            Instant createdAt,
            ProfileView profile,
            WalletView wallet
    ) {
    }
}
