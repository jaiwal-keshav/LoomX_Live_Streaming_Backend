package com.livestream.platform.auth;

import com.livestream.platform.notification.NotificationPreferenceEntity;
import com.livestream.platform.notification.NotificationPreferenceRepository;
import com.livestream.platform.shared.ApiException;
import com.livestream.platform.user.UserProfileEntity;
import com.livestream.platform.user.UserProfileRepository;
import com.livestream.platform.wallet.WalletEntity;
import com.livestream.platform.wallet.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final DeviceRepository devices;
    private final WalletRepository wallets;
    private final NotificationPreferenceRepository notificationPreferences;
    private final RefreshTokenRepository refreshTokens;
    private final OtpService otpService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, UserProfileRepository profiles, DeviceRepository devices,
                       WalletRepository wallets, NotificationPreferenceRepository notificationPreferences,
                       RefreshTokenRepository refreshTokens, OtpService otpService,
                       TokenService tokenService, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.profiles = profiles;
        this.devices = devices;
        this.wallets = wallets;
        this.notificationPreferences = notificationPreferences;
        this.refreshTokens = refreshTokens;
        this.otpService = otpService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthModels.TokenResponse registerPhone(AuthModels.PhoneRegistrationInput input,
                                                   String ipAddress, String userAgent) {
        OtpRequestEntity otp = otpService.verify(input.otpRequestId(), input.code(),
                OtpRequestEntity.Channel.SMS, OtpRequestEntity.Purpose.REGISTER);
        String phone = otp.destination();
        if (users.existsByPhoneNumber(phone)) {
            throw conflict("PHONE_IN_USE", "Phone number is already registered");
        }
        return createAccount(UserEntity.phoneUser(phone), input.profile(), input.device(), ipAddress, userAgent);
    }

    @Transactional
    public AuthModels.TokenResponse loginPhone(AuthModels.PhoneLoginInput input,
                                                String ipAddress, String userAgent) {
        OtpRequestEntity otp = otpService.verify(input.otpRequestId(), input.code(),
                OtpRequestEntity.Channel.SMS, OtpRequestEntity.Purpose.LOGIN);
        UserEntity user = users.findByPhoneNumber(otp.destination()).orElseThrow(this::invalidCredentials);
        return login(user, input.device(), ipAddress, userAgent);
    }

    @Transactional
    public AuthModels.TokenResponse registerEmail(AuthModels.EmailRegistrationInput input,
                                                   String ipAddress, String userAgent) {
        OtpRequestEntity otp = otpService.verify(input.otpRequestId(), input.code(),
                OtpRequestEntity.Channel.EMAIL, OtpRequestEntity.Purpose.REGISTER);
        String email = otp.destination();
        if (users.existsByEmailIgnoreCase(email)) {
            throw conflict("EMAIL_IN_USE", "Email address is already registered");
        }
        String password = InputNormalizer.password(input.password());
        return createAccount(UserEntity.emailUser(email, passwordEncoder.encode(password)),
                input.profile(), input.device(), ipAddress, userAgent);
    }

    @Transactional
    public AuthModels.TokenResponse loginEmail(AuthModels.EmailLoginInput input,
                                                String ipAddress, String userAgent) {
        UserEntity user = users.findByEmailIgnoreCase(InputNormalizer.email(input.email()))
                .orElseThrow(this::invalidCredentials);
        String password = InputNormalizer.password(input.password());
        if (user.passwordHash() == null || !passwordEncoder.matches(password, user.passwordHash())) {
            throw invalidCredentials();
        }
        return login(user, input.device(), ipAddress, userAgent);
    }

    @Transactional
    public void resetPassword(AuthModels.PasswordResetInput input) {
        OtpRequestEntity otp = otpService.verify(input.otpRequestId(), input.code(),
                OtpRequestEntity.Channel.EMAIL, OtpRequestEntity.Purpose.RESET_PASSWORD);
        UserEntity user = users.findByEmailIgnoreCase(otp.destination()).orElseThrow(this::invalidCredentials);
        user.changePassword(passwordEncoder.encode(InputNormalizer.password(input.newPassword())));
        refreshTokens.revokeUser(user.id(), Instant.now());
    }

    @Transactional
    public AuthModels.TokenResponse refresh(String rawRefreshToken, String ipAddress, String userAgent) {
        TokenService.RefreshResult result = tokenService.rotate(rawRefreshToken, ipAddress, userAgent);
        UserProfileEntity profile = profiles.findByUserId(result.user().id())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PROFILE_MISSING", "User profile is missing"));
        return response(result.user(), profile, result.tokens());
    }

    @Transactional(readOnly = true)
    public List<AuthModels.SessionResponse> sessions(UUID userId) {
        return devices.findByUserIdAndActiveTrueOrderByLastActiveAtDesc(userId).stream()
                .map(device -> new AuthModels.SessionResponse(
                        device.id(), device.deviceId(), device.deviceName(), device.appVersion(),
                        device.osVersion(), device.active(), device.lastActiveAt()))
                .toList();
    }

    private AuthModels.TokenResponse createAccount(UserEntity user, AuthModels.ProfileInput profileInput,
                                                    AuthModels.DeviceInput deviceInput,
                                                    String ipAddress, String userAgent) {
        String username = InputNormalizer.username(profileInput.username());
        if (profiles.existsByUsernameIgnoreCase(username)) {
            throw conflict("USERNAME_IN_USE", "Username is already in use");
        }
        users.saveAndFlush(user);
        UserProfileEntity profile = profiles.save(UserProfileEntity.create(
                user.id(), username, InputNormalizer.displayName(profileInput.displayName())));
        DeviceEntity device = devices.save(DeviceEntity.create(
                user.id(), deviceInput.deviceId(), deviceInput.deviceName(),
                deviceInput.appVersion(), deviceInput.osVersion(), deviceInput.fcmToken()));
        wallets.save(WalletEntity.empty(user.id()));
        notificationPreferences.save(NotificationPreferenceEntity.defaults(user.id()));
        TokenService.TokenPair pair = tokenService.issue(user, device, UUID.randomUUID(), ipAddress, userAgent);
        return response(user, profile, pair);
    }

    private AuthModels.TokenResponse login(UserEntity user, AuthModels.DeviceInput deviceInput,
                                           String ipAddress, String userAgent) {
        if (!user.canAuthenticate()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_UNAVAILABLE", "Account is not active");
        }
        user.recordLogin();
        DeviceEntity device = devices.findByUserIdAndDeviceId(user.id(), deviceInput.deviceId())
                .map(existing -> {
                    existing.update(deviceInput.deviceName(), deviceInput.appVersion(),
                            deviceInput.osVersion(), deviceInput.fcmToken());
                    return existing;
                })
                .orElseGet(() -> DeviceEntity.create(user.id(), deviceInput.deviceId(), deviceInput.deviceName(),
                        deviceInput.appVersion(), deviceInput.osVersion(), deviceInput.fcmToken()));
        device = devices.save(device);
        UserProfileEntity profile = profiles.findByUserId(user.id())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PROFILE_MISSING", "User profile is missing"));
        TokenService.TokenPair pair = tokenService.issue(user, device, UUID.randomUUID(), ipAddress, userAgent);
        return response(user, profile, pair);
    }

    private AuthModels.TokenResponse response(UserEntity user, UserProfileEntity profile, TokenService.TokenPair pair) {
        return new AuthModels.TokenResponse(pair.accessToken(), pair.refreshToken(), pair.accessTokenExpiresInSeconds(),
                new AuthModels.UserView(user.id(), profile.username(), profile.displayName(),
                        user.phoneNumber(), user.email(), user.roles()));
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Credentials are invalid");
    }

    private ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
