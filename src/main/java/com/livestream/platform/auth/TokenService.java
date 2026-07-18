package com.livestream.platform.auth;

import com.livestream.platform.config.AuthProperties;
import com.livestream.platform.shared.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class TokenService {
    public record TokenPair(String accessToken, String refreshToken, long accessTokenExpiresInSeconds) {
    }

    public record RefreshResult(UserEntity user, DeviceEntity device, TokenPair tokens) {
    }

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokens;
    private final UserRepository users;
    private final DeviceRepository devices;
    private final AuthProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(JwtEncoder jwtEncoder, RefreshTokenRepository refreshTokens,
                        UserRepository users, DeviceRepository devices, AuthProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.devices = devices;
        this.properties = properties;
    }

    @Transactional
    public TokenPair issue(UserEntity user, DeviceEntity device, UUID familyId,
                           String ipAddress, String userAgent) {
        Instant now = Instant.now();
        Instant accessExpiry = now.plus(properties.jwt().accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("live-streaming-backend")
                .issuedAt(now)
                .expiresAt(accessExpiry)
                .subject(user.id().toString())
                .claim("roles", user.roles().stream().map(Enum::name).sorted().toList())
                .claim("device_id", device.id().toString())
                .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        byte[] refreshBytes = new byte[32];
        secureRandom.nextBytes(refreshBytes);
        String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(refreshBytes);
        refreshTokens.save(RefreshTokenEntity.create(
                user.id(), device.id(), hash(rawRefreshToken), familyId,
                now.plus(properties.refreshTokenTtl()), ipAddress, userAgent));
        return new TokenPair(accessToken, rawRefreshToken, properties.jwt().accessTokenTtl().toSeconds());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ApiException.class)
    public RefreshResult rotate(String rawRefreshToken, String ipAddress, String userAgent) {
        RefreshTokenEntity current = refreshTokens.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(this::invalidRefreshToken);
        if (!current.active(Instant.now())) {
            refreshTokens.revokeFamily(current.tokenFamilyId(), Instant.now());
            throw invalidRefreshToken();
        }
        UserEntity user = users.findById(current.userId()).orElseThrow(this::invalidRefreshToken);
        DeviceEntity device = devices.findById(current.deviceId()).orElseThrow(this::invalidRefreshToken);
        if (!user.canAuthenticate() || !device.active()) {
            current.revoke();
            throw invalidRefreshToken();
        }
        current.revoke();
        return new RefreshResult(user, device,
                issue(user, device, current.tokenFamilyId(), ipAddress, userAgent));
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        refreshTokens.findByTokenHash(hash(rawRefreshToken)).ifPresent(RefreshTokenEntity::revoke);
    }

    @Transactional
    public void revokeAll(UUID userId) {
        refreshTokens.revokeUser(userId, Instant.now());
    }

    @Transactional
    public void revokeDevice(UUID userId, UUID deviceId) {
        DeviceEntity device = devices.findById(deviceId)
                .filter(found -> found.userId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Session was not found"));
        device.deactivate();
        refreshTokens.revokeDevice(userId, deviceId, Instant.now());
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID", "Refresh token is invalid or expired");
    }
}
