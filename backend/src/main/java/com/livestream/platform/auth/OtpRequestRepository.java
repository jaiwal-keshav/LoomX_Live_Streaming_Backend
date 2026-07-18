package com.livestream.platform.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpRequestRepository extends JpaRepository<OtpRequestEntity, UUID> {
    long countByDestinationAndCreatedAtAfter(String destination, Instant after);
    long countByIpAddressAndCreatedAtAfter(String ipAddress, Instant after);
    Optional<OtpRequestEntity> findTopByDestinationOrderByCreatedAtDesc(String destination);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from OtpRequestEntity request where request.id = :id")
    Optional<OtpRequestEntity> findByIdForUpdate(UUID id);
}
