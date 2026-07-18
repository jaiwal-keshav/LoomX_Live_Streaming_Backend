package com.livestream.platform.user;

import com.livestream.platform.auth.DeviceEntity;
import com.livestream.platform.auth.DeviceRepository;
import com.livestream.platform.auth.InputNormalizer;
import com.livestream.platform.auth.TokenService;
import com.livestream.platform.auth.UserEntity;
import com.livestream.platform.auth.UserRepository;
import com.livestream.platform.shared.ApiException;
import com.livestream.platform.wallet.WalletEntity;
import com.livestream.platform.wallet.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserProfileService {
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final WalletRepository wallets;
    private final DeviceRepository devices;
    private final TokenService tokens;

    public UserProfileService(UserRepository users, UserProfileRepository profiles,
                              WalletRepository wallets, DeviceRepository devices, TokenService tokens) {
        this.users = users;
        this.profiles = profiles;
        this.wallets = wallets;
        this.devices = devices;
        this.tokens = tokens;
    }

    @Transactional(readOnly = true)
    public ProfileModels.AccountView me(UUID userId) {
        UserEntity user = activeUser(userId);
        UserProfileEntity profile = profile(userId);
        WalletEntity wallet = wallets.findByUserId(userId)
                .orElseThrow(() -> invariant("WALLET_MISSING", "User wallet is missing"));
        return new ProfileModels.AccountView(
                user.id(), user.phoneNumber(), user.email(), user.loginProvider(), user.status(),
                user.roles(), user.createdAt(), view(profile),
                new ProfileModels.WalletView(wallet.coinBalance(), wallet.diamondBalance(),
                        wallet.subscriptionPointBalance()));
    }

    @Transactional(readOnly = true)
    public ProfileModels.ProfileView publicProfile(String username) {
        UserProfileEntity profile = profiles.findByUsernameIgnoreCase(InputNormalizer.username(username))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "Profile was not found"));
        activeUser(profile.userId());
        return view(profile);
    }

    @Transactional
    public ProfileModels.ProfileView update(UUID userId, ProfileModels.UpdateProfileInput input) {
        activeUser(userId);
        UserProfileEntity profile = profile(userId);

        String username = input.username() == null ? profile.username() : InputNormalizer.username(input.username());
        profiles.findByUsernameIgnoreCase(username)
                .filter(found -> !found.userId().equals(userId))
                .ifPresent(found -> {
                    throw new ApiException(HttpStatus.CONFLICT, "USERNAME_IN_USE", "Username is already in use");
                });
        String displayName = input.displayName() == null
                ? profile.displayName() : InputNormalizer.displayName(input.displayName());
        LocalDate dateOfBirth = input.dateOfBirth() == null ? profile.dateOfBirth() : input.dateOfBirth();
        if (dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_OF_BIRTH", "Date of birth cannot be in the future");
        }

        profile.update(
                username,
                displayName,
                input.bio() == null ? profile.bio() : blankToNull(input.bio()),
                input.avatarUrl() == null ? profile.avatarUrl() : InputNormalizer.optionalHttpsUrl(input.avatarUrl(), "avatarUrl"),
                input.coverImageUrl() == null ? profile.coverImageUrl() : InputNormalizer.optionalHttpsUrl(input.coverImageUrl(), "coverImageUrl"),
                input.gender() == null ? profile.gender() : blankToNull(input.gender()),
                dateOfBirth,
                input.countryCode() == null ? profile.countryCode() : input.countryCode().toUpperCase(Locale.ROOT),
                input.languageCode() == null ? profile.languageCode() : input.languageCode().toLowerCase(Locale.ROOT),
                input.privateProfile() == null ? profile.privateProfile() : input.privateProfile());
        return view(profile);
    }

    @Transactional
    public void delete(UUID userId) {
        UserEntity user = activeUser(userId);
        user.softDelete();
        devices.findByUserIdAndActiveTrueOrderByLastActiveAtDesc(userId).forEach(DeviceEntity::deactivate);
        tokens.revokeAll(userId);
    }

    private UserEntity activeUser(UUID userId) {
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found"));
        if (!user.canAuthenticate()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_UNAVAILABLE", "Account is not active");
        }
        return user;
    }

    private UserProfileEntity profile(UUID userId) {
        return profiles.findByUserId(userId)
                .orElseThrow(() -> invariant("PROFILE_MISSING", "User profile is missing"));
    }

    private ProfileModels.ProfileView view(UserProfileEntity profile) {
        return new ProfileModels.ProfileView(
                profile.userId(), profile.username(), profile.displayName(), profile.bio(),
                profile.avatarUrl(), profile.coverImageUrl(), profile.gender(), profile.dateOfBirth(),
                profile.countryCode(), profile.languageCode(), profile.privateProfile());
    }

    private String blankToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ApiException invariant(String code, String message) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, code, message);
    }
}
