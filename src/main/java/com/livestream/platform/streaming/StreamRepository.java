package com.livestream.platform.streaming;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

interface StreamRepository extends JpaRepository<StreamEntity, UUID> {
    boolean existsByHostIdAndStatus(UUID hostId, String status);

    @Query("""
            select stream from StreamEntity stream
            where stream.status = 'LIVE' and stream.streamType = 'PUBLIC'
              and (:categoryId is null or stream.categoryId = :categoryId)
            order by stream.startedAt desc
            """)
    Page<StreamEntity> discover(UUID categoryId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stream from StreamEntity stream where stream.id = :id")
    Optional<StreamEntity> findByIdForUpdate(UUID id);

    @Query("""
            select count(block) from UserBlockEntity block
            where (block.blockerId = :first and block.blockedUserId = :second)
               or (block.blockerId = :second and block.blockedUserId = :first)
            """)
    long countBlocksBetween(UUID first, UUID second);
}
