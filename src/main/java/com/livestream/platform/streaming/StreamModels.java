package com.livestream.platform.streaming;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class StreamModels {
    private StreamModels() {
    }

    public record CreateStreamInput(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @NotNull UUID categoryId,
            String thumbnailUrl
    ) {
    }

    public record InvitationInput(@NotNull UUID inviteeId) {
    }

    public enum RequestDecision { ACCEPTED, REJECTED }
    public enum InvitationDecision { ACCEPTED, DECLINED }

    public record RequestDecisionInput(@NotNull RequestDecision status) {
    }

    public record InvitationDecisionInput(@NotNull InvitationDecision status) {
    }

    public record CategoryView(UUID id, String name, String iconUrl, String bannerUrl, int displayOrder) {
    }

    public record UserView(UUID id, String username, String displayName, String avatarUrl) {
    }

    public record BroadcasterView(int slotNumber, String role, UserView user) {
    }

    public record StreamView(
            UUID id,
            UserView host,
            CategoryView category,
            String title,
            String description,
            String thumbnailUrl,
            String status,
            int maxBroadcasters,
            long currentViewerCount,
            long totalLikeCount,
            long totalWatchSeconds,
            long totalUniqueViewers,
            Instant startedAt,
            Instant endedAt,
            String myParticipantType,
            boolean liked,
            String myJoinRequestStatus,
            List<BroadcasterView> broadcasters
    ) {
    }

    public record PageView<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
    }

    public record RtcCredentials(
            String appId,
            String channelName,
            String userAccount,
            String token,
            String role,
            Instant expiresAt
    ) {
    }

    public record StreamSession(StreamView stream, RtcCredentials rtc) {
    }

    public record LikeView(boolean liked, long totalLikeCount) {
    }

    public record JoinRequestView(
            UUID id,
            UUID streamId,
            UserView requester,
            String status,
            Instant requestedAt,
            Instant respondedAt
    ) {
    }

    public record InvitationView(
            UUID id,
            UUID streamId,
            String streamTitle,
            UserView inviter,
            UserView invitee,
            String status,
            Instant sentAt,
            Instant respondedAt
    ) {
    }
}
