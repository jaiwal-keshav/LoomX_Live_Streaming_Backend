package com.livestream.platform.streaming;

import com.livestream.platform.auth.InputNormalizer;
import com.livestream.platform.auth.UserEntity;
import com.livestream.platform.auth.UserRepository;
import com.livestream.platform.shared.ApiException;
import com.livestream.platform.user.UserProfileEntity;
import com.livestream.platform.user.UserProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class StreamingService {
    private static final String LIVE = "LIVE";
    private static final String PENDING = "PENDING";
    private static final String HOST = "HOST";
    private static final String CO_HOST = "CO_HOST";
    private static final String VIEWER = "VIEWER";

    private final StreamCategoryRepository categories;
    private final StreamRepository streams;
    private final StreamParticipantRepository participants;
    private final StreamJoinRequestRepository requests;
    private final StreamInvitationRepository invitations;
    private final StreamBroadcasterSlotRepository slots;
    private final StreamLikeRepository likes;
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final AgoraTokenService agora;

    public StreamingService(StreamCategoryRepository categories, StreamRepository streams,
                            StreamParticipantRepository participants, StreamJoinRequestRepository requests,
                            StreamInvitationRepository invitations, StreamBroadcasterSlotRepository slots,
                            StreamLikeRepository likes, UserRepository users,
                            UserProfileRepository profiles, AgoraTokenService agora) {
        this.categories = categories;
        this.streams = streams;
        this.participants = participants;
        this.requests = requests;
        this.invitations = invitations;
        this.slots = slots;
        this.likes = likes;
        this.users = users;
        this.profiles = profiles;
        this.agora = agora;
    }

    @Transactional(readOnly = true)
    public List<StreamModels.CategoryView> categories() {
        return categories.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(this::categoryView)
                .toList();
    }

    @Transactional(readOnly = true)
    public StreamModels.PageView<StreamModels.StreamView> discover(UUID userId, UUID categoryId, int page, int size) {
        activeUser(userId);
        if (categoryId != null) activeCategory(categoryId);
        Page<StreamEntity> result = streams.discover(categoryId, PageRequest.of(page, size));
        // ponytail: per-stream assembly is acceptable at a capped page size of 50; batch projections when measured.
        List<StreamModels.StreamView> items = result.getContent().stream()
                .map(stream -> view(userId, stream))
                .toList();
        return new StreamModels.PageView<>(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public StreamModels.StreamView stream(UUID userId, UUID streamId) {
        activeUser(userId);
        return view(userId, stream(streamId));
    }

    @Transactional
    public StreamModels.StreamSession start(UUID userId, StreamModels.CreateStreamInput input) {
        agora.requireConfigured();
        activeUser(userId);
        if (streams.existsByHostIdAndStatus(userId, LIVE)) {
            throw conflict("LIVE_STREAM_ALREADY_EXISTS", "The user already has a live stream");
        }
        StreamCategoryEntity category = activeCategory(input.categoryId());
        Instant now = Instant.now();
        String roomId = "ls_" + UUID.randomUUID().toString().replace("-", "");
        StreamEntity stream = streams.save(StreamEntity.start(
                roomId, userId, category.id(), input.title().trim(), trimToNull(input.description()),
                InputNormalizer.optionalHttpsUrl(input.thumbnailUrl(), "thumbnailUrl"), now));

        List<StreamBroadcasterSlotEntity> broadcasterSlots = IntStream.rangeClosed(1, 5)
                .mapToObj(number -> StreamBroadcasterSlotEntity.empty(stream.id(), number))
                .toList();
        broadcasterSlots.getFirst().occupy(userId, HOST, now);
        slots.saveAll(broadcasterSlots);
        participants.save(StreamParticipantEntity.join(stream.id(), userId, HOST, now));
        return session(userId, stream, HOST);
    }

    @Transactional
    public StreamModels.StreamView end(UUID userId, UUID streamId) {
        activeUser(userId);
        StreamEntity stream = lockedStream(streamId);
        requireHost(userId, stream);
        if (!stream.live()) return view(userId, stream);

        Instant now = Instant.now();
        for (StreamParticipantEntity participant : participants.findByStreamIdAndLeftAtIsNull(streamId)) {
            if (VIEWER.equals(participant.participantType())) {
                stream.viewerLeft(watchSeconds(participant, now));
            }
            participant.leave(now);
        }
        for (StreamBroadcasterSlotEntity slot : slots.findByStreamIdForUpdate(streamId)) {
            if (!slot.available()) slot.clear(now);
        }
        requests.findByStreamIdAndStatusOrderByRequestedAtAsc(streamId, PENDING)
                .forEach(request -> request.expire(now));
        invitations.findByStreamIdAndStatus(streamId, PENDING)
                .forEach(invitation -> invitation.expire(now));
        stream.end(now);
        return view(userId, stream);
    }

    @Transactional
    public StreamModels.StreamSession join(UUID userId, UUID streamId) {
        activeUser(userId);
        StreamEntity stream = lockedStream(streamId);
        requireLive(stream);
        ensureUnblocked(userId, stream.hostId());

        StreamParticipantEntity participant = participants
                .findByStreamIdAndUserIdAndLeftAtIsNull(streamId, userId)
                .orElse(null);
        if (participant == null) {
            boolean firstVisit = !participants.existsByStreamIdAndUserIdAndParticipantType(streamId, userId, VIEWER);
            participant = participants.save(StreamParticipantEntity.join(streamId, userId, VIEWER, Instant.now()));
            stream.viewerJoined(firstVisit);
        }
        return session(userId, stream, participant.participantType());
    }

    @Transactional
    public void leave(UUID userId, UUID streamId) {
        activeUser(userId);
        StreamEntity stream = lockedStream(streamId);
        StreamParticipantEntity participant = activeParticipant(streamId, userId);
        if (HOST.equals(participant.participantType())) {
            throw conflict("HOST_MUST_END_STREAM", "The host must end the stream instead of leaving it");
        }
        Instant now = Instant.now();
        if (VIEWER.equals(participant.participantType())) {
            stream.viewerLeft(watchSeconds(participant, now));
        } else {
            clearSlot(streamId, userId, now);
        }
        participant.leave(now);
    }

    @Transactional(readOnly = true)
    public StreamModels.RtcCredentials token(UUID userId, UUID streamId) {
        activeUser(userId);
        StreamEntity stream = stream(streamId);
        requireLive(stream);
        StreamParticipantEntity participant = activeParticipant(streamId, userId);
        return agora.issue(stream.roomId(), userId, publishes(participant.participantType()));
    }

    @Transactional
    public StreamModels.LikeView like(UUID userId, UUID streamId) {
        activeUser(userId);
        StreamEntity stream = lockedStream(streamId);
        requireLive(stream);
        activeParticipant(streamId, userId);
        if (!likes.existsByStreamIdAndUserId(streamId, userId)) {
            likes.save(StreamLikeEntity.create(streamId, userId));
            stream.likeAdded();
        }
        return new StreamModels.LikeView(true, stream.totalLikeCount());
    }

    @Transactional
    public StreamModels.LikeView unlike(UUID userId, UUID streamId) {
        activeUser(userId);
        StreamEntity stream = lockedStream(streamId);
        likes.findByStreamIdAndUserId(streamId, userId).ifPresent(like -> {
            likes.delete(like);
            stream.likeRemoved();
        });
        return new StreamModels.LikeView(false, stream.totalLikeCount());
    }

    @Transactional
    public StreamModels.JoinRequestView requestCohost(UUID userId, UUID streamId) {
        activeUser(userId);
        StreamEntity stream = lockedStream(streamId);
        requireLive(stream);
        ensureUnblocked(userId, stream.hostId());
        StreamParticipantEntity participant = activeParticipant(streamId, userId);
        if (!VIEWER.equals(participant.participantType())) {
            throw conflict("COHOST_REQUEST_NOT_ALLOWED", "Only an active viewer can request a co-host slot");
        }
        StreamJoinRequestEntity request = requests
                .findByStreamIdAndRequesterIdAndStatus(streamId, userId, PENDING)
                .orElseGet(() -> requests.save(StreamJoinRequestEntity.pending(streamId, userId, Instant.now())));
        return requestView(request);
    }

    @Transactional(readOnly = true)
    public List<StreamModels.JoinRequestView> joinRequests(UUID userId, UUID streamId) {
        activeUser(userId);
        StreamEntity stream = stream(streamId);
        requireHost(userId, stream);
        return requests.findByStreamIdAndStatusOrderByRequestedAtAsc(streamId, PENDING).stream()
                .map(this::requestView)
                .toList();
    }

    @Transactional
    public StreamModels.JoinRequestView decideRequest(UUID userId, UUID streamId, UUID requestId,
                                                      StreamModels.RequestDecision decision) {
        activeUser(userId);
        StreamEntity stream = lockedStream(streamId);
        requireHost(userId, stream);
        requireLive(stream);
        StreamJoinRequestEntity request = requests.findById(requestId)
                .filter(found -> found.streamId().equals(streamId))
                .orElseThrow(() -> notFound("JOIN_REQUEST_NOT_FOUND", "Join request was not found"));
        requirePending(request.status(), "Join request");
        if (decision == StreamModels.RequestDecision.ACCEPTED) {
            promote(stream, request.requesterId(), slots.findByStreamIdForUpdate(streamId), Instant.now(), true);
        }
        request.respond(decision.name(), userId, Instant.now());
        return requestView(request);
    }

    @Transactional
    public StreamModels.InvitationView invite(UUID userId, UUID streamId, UUID inviteeId) {
        activeUser(userId);
        activeUser(inviteeId);
        StreamEntity stream = lockedStream(streamId);
        requireHost(userId, stream);
        requireLive(stream);
        if (userId.equals(inviteeId)) {
            throw conflict("INVITATION_NOT_ALLOWED", "The host cannot invite themselves");
        }
        ensureUnblocked(userId, inviteeId);
        StreamInvitationEntity invitation = invitations
                .findByStreamIdAndInviteeIdAndStatus(streamId, inviteeId, PENDING)
                .orElseGet(() -> invitations.save(
                        StreamInvitationEntity.pending(streamId, userId, inviteeId, Instant.now())));
        return invitationView(invitation);
    }

    @Transactional(readOnly = true)
    public List<StreamModels.InvitationView> invitations(UUID userId, String status) {
        activeUser(userId);
        if (!PENDING.equalsIgnoreCase(status)) {
            throw badRequest("INVALID_INVITATION_STATUS", "Only PENDING invitations can be listed in Phase 3A");
        }
        return invitations.findByInviteeIdAndStatusOrderBySentAtDesc(userId, PENDING).stream()
                .map(this::invitationView)
                .toList();
    }

    @Transactional
    public StreamModels.InvitationView decideInvitation(UUID userId, UUID invitationId,
                                                        StreamModels.InvitationDecision decision) {
        activeUser(userId);
        StreamInvitationEntity invitation = invitations.findById(invitationId)
                .filter(found -> found.inviteeId().equals(userId))
                .orElseThrow(() -> notFound("INVITATION_NOT_FOUND", "Invitation was not found"));
        StreamEntity stream = lockedStream(invitation.streamId());
        requireLive(stream);
        requirePending(invitation.status(), "Invitation");
        ensureUnblocked(userId, stream.hostId());
        if (decision == StreamModels.InvitationDecision.ACCEPTED) {
            promote(stream, userId, slots.findByStreamIdForUpdate(stream.id()), Instant.now(), false);
        }
        invitation.respond(decision.name(), Instant.now());
        return invitationView(invitation);
    }

    @Transactional
    public void removeCohost(UUID hostId, UUID streamId, UUID cohostId) {
        activeUser(hostId);
        StreamEntity stream = lockedStream(streamId);
        requireHost(hostId, stream);
        requireLive(stream);
        StreamParticipantEntity participant = activeParticipant(streamId, cohostId);
        if (!CO_HOST.equals(participant.participantType())) {
            throw conflict("USER_IS_NOT_COHOST", "The user is not an active co-host");
        }
        Instant now = Instant.now();
        participant.leave(now);
        clearSlot(streamId, cohostId, now);
    }

    private void promote(StreamEntity stream, UUID userId, List<StreamBroadcasterSlotEntity> lockedSlots,
                         Instant now, boolean viewerRequired) {
        StreamParticipantEntity participant = participants
                .findByStreamIdAndUserIdAndLeftAtIsNull(stream.id(), userId)
                .orElse(null);
        if (participant != null && CO_HOST.equals(participant.participantType())) return;
        if (viewerRequired && (participant == null || !VIEWER.equals(participant.participantType()))) {
            throw conflict("VIEWER_NOT_ACTIVE", "The requester is no longer an active viewer");
        }
        if (participant != null) {
            if (HOST.equals(participant.participantType())) {
                throw conflict("COHOST_NOT_ALLOWED", "The host cannot become a co-host");
            }
            if (VIEWER.equals(participant.participantType())) {
                stream.viewerLeft(watchSeconds(participant, now));
            }
            participant.leave(now);
            participants.saveAndFlush(participant);
        }
        StreamBroadcasterSlotEntity slot = lockedSlots.stream()
                .filter(found -> found.slotNumber() > 1 && found.available())
                .findFirst()
                .orElseThrow(() -> conflict("BROADCASTER_LIMIT_REACHED", "All co-host slots are occupied"));
        slot.occupy(userId, CO_HOST, now);
        participants.save(StreamParticipantEntity.join(stream.id(), userId, CO_HOST, now));
    }

    private void clearSlot(UUID streamId, UUID userId, Instant now) {
        slots.findByStreamIdForUpdate(streamId).stream()
                .filter(slot -> userId.equals(slot.occupiedBy()))
                .findFirst()
                .orElseThrow(() -> conflict("BROADCASTER_SLOT_MISSING", "The broadcaster slot is missing"))
                .clear(now);
    }

    private StreamModels.StreamSession session(UUID userId, StreamEntity stream, String participantType) {
        return new StreamModels.StreamSession(
                view(userId, stream), agora.issue(stream.roomId(), userId, publishes(participantType)));
    }

    private boolean publishes(String participantType) {
        return HOST.equals(participantType) || CO_HOST.equals(participantType);
    }

    private StreamModels.StreamView view(UUID currentUserId, StreamEntity stream) {
        StreamCategoryEntity category = stream.categoryId() == null ? null
                : categories.findById(stream.categoryId()).orElse(null);
        UserProfileEntity host = profile(stream.hostId());
        List<StreamBroadcasterSlotEntity> occupiedSlots = slots.findByStreamIdOrderBySlotNumber(stream.id()).stream()
                .filter(slot -> !slot.available())
                .toList();
        List<UUID> broadcasterIds = occupiedSlots.stream().map(StreamBroadcasterSlotEntity::occupiedBy).toList();
        Map<UUID, UserProfileEntity> broadcasterProfiles = profileMap(broadcasterIds);
        List<StreamModels.BroadcasterView> broadcasters = occupiedSlots.stream()
                .map(slot -> new StreamModels.BroadcasterView(
                        slot.slotNumber(), slot.role(), userView(requireProfile(broadcasterProfiles, slot.occupiedBy()))))
                .toList();
        String participantType = participants.findByStreamIdAndUserIdAndLeftAtIsNull(stream.id(), currentUserId)
                .map(StreamParticipantEntity::participantType)
                .orElse(null);
        String requestStatus = requests.findTopByStreamIdAndRequesterIdOrderByRequestedAtDesc(stream.id(), currentUserId)
                .map(StreamJoinRequestEntity::status)
                .orElse(null);
        return new StreamModels.StreamView(
                stream.id(), userView(host), category == null ? null : categoryView(category),
                stream.title(), stream.description(), stream.thumbnailUrl(), stream.status(),
                stream.maxBroadcasters(), stream.currentViewerCount(), stream.totalLikeCount(),
                stream.totalWatchSeconds(), stream.totalUniqueViewers(), stream.startedAt(), stream.endedAt(),
                participantType, likes.existsByStreamIdAndUserId(stream.id(), currentUserId), requestStatus,
                broadcasters);
    }

    private StreamModels.JoinRequestView requestView(StreamJoinRequestEntity request) {
        return new StreamModels.JoinRequestView(
                request.id(), request.streamId(), userView(profile(request.requesterId())),
                request.status(), request.requestedAt(), request.respondedAt());
    }

    private StreamModels.InvitationView invitationView(StreamInvitationEntity invitation) {
        StreamEntity stream = stream(invitation.streamId());
        return new StreamModels.InvitationView(
                invitation.id(), invitation.streamId(), stream.title(),
                userView(profile(invitation.inviterId())), userView(profile(invitation.inviteeId())),
                invitation.status(), invitation.sentAt(), invitation.respondedAt());
    }

    private StreamModels.CategoryView categoryView(StreamCategoryEntity category) {
        return new StreamModels.CategoryView(
                category.id(), category.name(), category.iconUrl(), category.bannerUrl(), category.displayOrder());
    }

    private StreamModels.UserView userView(UserProfileEntity profile) {
        return new StreamModels.UserView(
                profile.userId(), profile.username(), profile.displayName(), profile.avatarUrl());
    }

    private Map<UUID, UserProfileEntity> profileMap(List<UUID> userIds) {
        Map<UUID, UserProfileEntity> result = new HashMap<>();
        profiles.findByUserIdIn(userIds).forEach(profile -> result.put(profile.userId(), profile));
        return result;
    }

    private UserProfileEntity requireProfile(Map<UUID, UserProfileEntity> profileMap, UUID userId) {
        UserProfileEntity profile = profileMap.get(userId);
        if (profile == null) throw invariant("PROFILE_MISSING", "User profile is missing");
        return profile;
    }

    private UserProfileEntity profile(UUID userId) {
        return profiles.findByUserId(userId)
                .orElseThrow(() -> invariant("PROFILE_MISSING", "User profile is missing"));
    }

    private UserEntity activeUser(UUID userId) {
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> notFound("USER_NOT_FOUND", "User was not found"));
        if (!user.canAuthenticate()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_UNAVAILABLE", "Account is not active");
        }
        return user;
    }

    private StreamCategoryEntity activeCategory(UUID categoryId) {
        return categories.findByIdAndActiveTrue(categoryId)
                .orElseThrow(() -> notFound("STREAM_CATEGORY_NOT_FOUND", "Active stream category was not found"));
    }

    private StreamEntity stream(UUID streamId) {
        return streams.findById(streamId)
                .orElseThrow(() -> notFound("STREAM_NOT_FOUND", "Stream was not found"));
    }

    private StreamEntity lockedStream(UUID streamId) {
        return streams.findByIdForUpdate(streamId)
                .orElseThrow(() -> notFound("STREAM_NOT_FOUND", "Stream was not found"));
    }

    private StreamParticipantEntity activeParticipant(UUID streamId, UUID userId) {
        return participants.findByStreamIdAndUserIdAndLeftAtIsNull(streamId, userId)
                .orElseThrow(() -> conflict("PARTICIPANT_NOT_ACTIVE", "The user is not active in the stream"));
    }

    private void requireHost(UUID userId, StreamEntity stream) {
        if (!stream.hostId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "HOST_REQUIRED", "Only the stream host may perform this action");
        }
    }

    private void requireLive(StreamEntity stream) {
        if (!stream.live()) throw conflict("STREAM_NOT_LIVE", "The stream is not live");
    }

    private void requirePending(String status, String resource) {
        if (!PENDING.equals(status)) throw conflict("ALREADY_RESPONDED", resource + " is no longer pending");
    }

    private void ensureUnblocked(UUID first, UUID second) {
        if (!first.equals(second) && streams.countBlocksBetween(first, second) > 0) {
            throw new ApiException(HttpStatus.FORBIDDEN, "BLOCKED_RELATIONSHIP",
                    "This action is unavailable because one user has blocked the other");
        }
    }

    private long watchSeconds(StreamParticipantEntity participant, Instant now) {
        return Math.max(0, Duration.between(participant.joinedAt(), now).toSeconds());
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    private ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    private ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    private ApiException invariant(String code, String message) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, code, message);
    }
}
