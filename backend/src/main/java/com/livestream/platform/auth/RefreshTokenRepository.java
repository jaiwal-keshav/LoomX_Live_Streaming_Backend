package com.livestream.platform.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
    List<RefreshTokenEntity> findByUserIdAndRevokedAtIsNull(UUID userId);

    @Modifying
    @Query("update RefreshTokenEntity token set token.revokedAt = :now where token.tokenFamilyId = :familyId and token.revokedAt is null")
    int revokeFamily(UUID familyId, Instant now);

    @Modifying
    @Query("update RefreshTokenEntity token set token.revokedAt = :now where token.userId = :userId and token.revokedAt is null")
    int revokeUser(UUID userId, Instant now);

    @Modifying
    @Query("update RefreshTokenEntity token set token.revokedAt = :now where token.userId = :userId and token.deviceId = :deviceId and token.revokedAt is null")
    int revokeDevice(UUID userId, UUID deviceId, Instant now);
}
