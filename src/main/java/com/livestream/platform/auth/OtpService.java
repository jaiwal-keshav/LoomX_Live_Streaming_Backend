package com.livestream.platform.auth;

import com.livestream.platform.config.AuthProperties;
import com.livestream.platform.shared.ApiException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class OtpService {
    private final OtpRequestRepository repository;
    private final AuthProperties properties;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpRequestRepository repository, AuthProperties properties, Environment environment) {
        this.repository = repository;
        this.properties = properties;
        if (properties.otp().exposeCode() && !environment.acceptsProfiles(Profiles.of("dev", "test"))) {
            throw new IllegalStateException("OTP codes may only be exposed in dev or test profiles");
        }
    }

    @Transactional
    public AuthModels.OtpResponse request(AuthModels.OtpInput input, String ipAddress) {
        String destination = normalize(input.channel(), input.destination());
        validateChannelPurpose(input.channel(), input.purpose());
        Instant now = Instant.now();
        Instant windowStart = now.minus(properties.otp().rateWindow());

        repository.findTopByDestinationOrderByCreatedAtDesc(destination).ifPresent(latest -> {
            if (latest.createdAt() != null
                    && latest.createdAt().plus(properties.otp().resendCooldown()).isAfter(now)) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_COOLDOWN", "Wait before requesting another OTP");
            }
        });
        if (repository.countByDestinationAndCreatedAtAfter(destination, windowStart)
                >= properties.otp().maxSendsPerDestination()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_RATE_LIMITED", "Too many OTP requests");
        }
        if (ipAddress != null && repository.countByIpAddressAndCreatedAtAfter(ipAddress, windowStart)
                >= properties.otp().maxSendsPerIp()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_RATE_LIMITED", "Too many OTP requests");
        }

        String code = "%06d".formatted(random.nextInt(1_000_000));
        Instant expiresAt = now.plus(properties.otp().expiresIn());
        OtpRequestEntity request = OtpRequestEntity.create(
                destination, input.channel(), input.purpose(), "pending", properties.otp().maxAttempts(),
                expiresAt, ipAddress);
        setHash(request, code);
        repository.save(request);
        return new AuthModels.OtpResponse(request.id(), expiresAt,
                properties.otp().exposeCode() ? code : null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ApiException.class)
    public OtpRequestEntity verify(UUID requestId, String code, OtpRequestEntity.Channel channel,
                                   OtpRequestEntity.Purpose purpose) {
        OtpRequestEntity request = repository.findByIdForUpdate(requestId)
                .orElseThrow(this::invalidOtp);
        if (request.channel() != channel || request.purpose() != purpose || !request.isUsable(Instant.now())) {
            throw invalidOtp();
        }
        byte[] expected = HexFormat.of().parseHex(request.otpHash());
        byte[] supplied = HexFormat.of().parseHex(hash(request.id(), code));
        if (!MessageDigest.isEqual(expected, supplied)) {
            request.failedAttempt();
            throw invalidOtp();
        }
        request.verify();
        return request;
    }

    private void setHash(OtpRequestEntity request, String code) {
        request.setOtpHash(hash(request.id(), code));
    }

    private String hash(UUID requestId, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.otp().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((requestId + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash OTP", exception);
        }
    }

    private String normalize(OtpRequestEntity.Channel channel, String destination) {
        return channel == OtpRequestEntity.Channel.SMS
                ? InputNormalizer.phone(destination)
                : InputNormalizer.email(destination);
    }

    private void validateChannelPurpose(OtpRequestEntity.Channel channel, OtpRequestEntity.Purpose purpose) {
        boolean allowed = channel == OtpRequestEntity.Channel.SMS
                ? purpose == OtpRequestEntity.Purpose.LOGIN || purpose == OtpRequestEntity.Purpose.REGISTER
                : purpose == OtpRequestEntity.Purpose.REGISTER || purpose == OtpRequestEntity.Purpose.RESET_PASSWORD;
        if (!allowed) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OTP_PURPOSE", "OTP channel and purpose are incompatible");
        }
    }

    private ApiException invalidOtp() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "OTP_INVALID", "OTP is invalid or expired");
    }
}
