package com.livestream.platform.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class AuthModels {
    private AuthModels() {
    }

    public record OtpInput(
            @NotNull OtpRequestEntity.Channel channel,
            @NotNull OtpRequestEntity.Purpose purpose,
            @NotBlank @Size(max = 255) String destination
    ) {
    }

    public record OtpResponse(UUID requestId, Instant expiresAt, String debugOtp) {
    }

    public record DeviceInput(
            @NotBlank @Size(max = 255) String deviceId,
            @Size(max = 100) String deviceName,
            @Size(max = 30) String appVersion,
            @Size(max = 30) String osVersion,
            String fcmToken
    ) {
    }

    public record ProfileInput(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,30}") String username,
            @NotBlank @Size(max = 100) String displayName
    ) {
    }

    public record PhoneRegistrationInput(
            @NotNull UUID otpRequestId,
            @NotBlank @Pattern(regexp = "\\d{6}") String code,
            @Valid @NotNull ProfileInput profile,
            @Valid @NotNull DeviceInput device
    ) {
    }

    public record PhoneLoginInput(
            @NotNull UUID otpRequestId,
            @NotBlank @Pattern(regexp = "\\d{6}") String code,
            @Valid @NotNull DeviceInput device
    ) {
    }

    public record EmailRegistrationInput(
            @NotNull UUID otpRequestId,
            @NotBlank @Pattern(regexp = "\\d{6}") String code,
            @NotBlank String password,
            @Valid @NotNull ProfileInput profile,
            @Valid @NotNull DeviceInput device
    ) {
    }

    public record EmailLoginInput(
            @NotBlank @Size(max = 255) String email,
            @NotBlank String password,
            @Valid @NotNull DeviceInput device
    ) {
    }

    public record PasswordResetInput(
            @NotNull UUID otpRequestId,
            @NotBlank @Pattern(regexp = "\\d{6}") String code,
            @NotBlank String newPassword
    ) {
    }

    public record RefreshInput(@NotBlank @Size(max = 200) String refreshToken) {
    }

    public record UserView(
            UUID id,
            String username,
            String displayName,
            String phoneNumber,
            String email,
            Set<UserEntity.Role> roles
    ) {
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresInSeconds,
            UserView user
    ) {
    }

    public record SessionResponse(
            UUID id,
            String deviceId,
            String deviceName,
            String appVersion,
            String osVersion,
            boolean active,
            Instant lastActiveAt
    ) {
    }
}
