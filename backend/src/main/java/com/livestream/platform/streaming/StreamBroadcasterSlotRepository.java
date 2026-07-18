package com.livestream.platform.streaming;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

interface StreamBroadcasterSlotRepository extends JpaRepository<StreamBroadcasterSlotEntity, UUID> {
    List<StreamBroadcasterSlotEntity> findByStreamIdOrderBySlotNumber(UUID streamId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select slot from StreamBroadcasterSlotEntity slot where slot.streamId = :streamId order by slot.slotNumber")
    List<StreamBroadcasterSlotEntity> findByStreamIdForUpdate(UUID streamId);
}
