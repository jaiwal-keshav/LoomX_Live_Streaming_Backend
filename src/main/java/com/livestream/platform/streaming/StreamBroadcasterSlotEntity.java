package com.livestream.platform.streaming;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stream_broadcaster_slots")
public class StreamBroadcasterSlotEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "stream_id", nullable = false)
    UUID streamId;
    @Column(name = "slot_number", nullable = false)
    int slotNumber;
    @Column(name = "occupied_by")
    UUID occupiedBy;
    @Column(length = 20)
    String role;
    @Column(name = "joined_at")
    Instant joinedAt;
    @Column(name = "left_at")
    Instant leftAt;

    protected StreamBroadcasterSlotEntity() {
    }

    public static StreamBroadcasterSlotEntity empty(UUID streamId, int slotNumber) {
        StreamBroadcasterSlotEntity slot = new StreamBroadcasterSlotEntity();
        slot.streamId = streamId;
        slot.slotNumber = slotNumber;
        return slot;
    }

    public UUID id() { return id; }
    public int slotNumber() { return slotNumber; }
    public UUID occupiedBy() { return occupiedBy; }
    public String role() { return role; }
    public boolean available() { return occupiedBy == null; }

    public void occupy(UUID userId, String role, Instant now) {
        occupiedBy = userId;
        this.role = role;
        joinedAt = now;
        leftAt = null;
    }

    public void clear(Instant now) {
        occupiedBy = null;
        role = null;
        joinedAt = null;
        leftAt = now;
    }
}
