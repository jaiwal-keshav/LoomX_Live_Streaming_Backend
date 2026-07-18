package com.livestream.platform.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        @Valid @NotNull Jwt jwt,
        @NotNull Duration refreshTokenTtl,
        @Valid @NotNull Otp otp
) {
    public record Jwt(@NotBlank String secret, @NotNull Duration accessTokenTtl) {
    }

    public record Otp(
            @NotBlank String secret,
            @NotNull Duration expiresIn,
            @NotNull Duration resendCooldown,
            @Min(1) @Max(10) int maxAttempts,
            @Min(1) int maxSendsPerDestination,
            @Min(1) int maxSendsPerIp,
            @NotNull Duration rateWindow,
            boolean exposeCode
    ) {
    }
}
