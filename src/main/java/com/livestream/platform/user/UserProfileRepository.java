package com.livestream.platform.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {
    Optional<UserProfileEntity> findByUserId(UUID userId);
    Optional<UserProfileEntity> findByUsernameIgnoreCase(String username);
    List<UserProfileEntity> findByUserIdIn(Iterable<UUID> userIds);
    boolean existsByUsernameIgnoreCase(String username);
}
