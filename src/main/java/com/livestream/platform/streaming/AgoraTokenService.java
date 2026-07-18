package com.livestream.platform.streaming;

import com.livestream.platform.config.AgoraProperties;
import com.livestream.platform.shared.ApiException;
import io.agora.media.RtcTokenBuilder2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AgoraTokenService {
    private final AgoraProperties properties;

    public AgoraTokenService(AgoraProperties properties) {
        this.properties = properties;
    }

    public void requireConfigured() {
        if (!StringUtils.hasText(properties.appId()) || !StringUtils.hasText(properties.appCertificate())) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AGORA_NOT_CONFIGURED",
                    "Agora App ID and App Certificate are not configured");
        }
    }

    public StreamModels.RtcCredentials issue(String channelName, UUID userId, boolean publisher) {
        requireConfigured();
        int ttlSeconds = ttlSeconds();
        RtcTokenBuilder2.Role role = publisher
                ? RtcTokenBuilder2.Role.ROLE_PUBLISHER
                : RtcTokenBuilder2.Role.ROLE_SUBSCRIBER;
        String token = new RtcTokenBuilder2().buildTokenWithUserAccount(
                properties.appId(), properties.appCertificate(), channelName, userId.toString(),
                role, ttlSeconds, ttlSeconds);
        return new StreamModels.RtcCredentials(
                properties.appId(), channelName, userId.toString(), token,
                publisher ? "BROADCASTER" : "AUDIENCE", Instant.now().plusSeconds(ttlSeconds));
    }

    private int ttlSeconds() {
        Duration ttl = properties.tokenTtl() == null ? Duration.ofMinutes(15) : properties.tokenTtl();
        long seconds = ttl.toSeconds();
        if (seconds < 60 || seconds > Duration.ofHours(24).toSeconds()) {
            throw new IllegalStateException("AGORA_TOKEN_TTL must be between 1 minute and 24 hours");
        }
        return Math.toIntExact(seconds);
    }
}
