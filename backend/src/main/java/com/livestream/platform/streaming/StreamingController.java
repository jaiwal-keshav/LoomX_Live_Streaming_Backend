package com.livestream.platform.streaming;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1")
public class StreamingController {
    private final StreamingService service;

    public StreamingController(StreamingService service) {
        this.service = service;
    }

    @GetMapping("/stream-categories")
    List<StreamModels.CategoryView> categories() {
        return service.categories();
    }

    @GetMapping("/streams")
    StreamModels.PageView<StreamModels.StreamView> discover(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.discover(userId(jwt), categoryId, page, size);
    }

    @GetMapping("/streams/{streamId}")
    StreamModels.StreamView stream(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID streamId) {
        return service.stream(userId(jwt), streamId);
    }

    @PostMapping("/streams")
    ResponseEntity<StreamModels.StreamSession> start(@AuthenticationPrincipal Jwt jwt,
                                                     @Valid @RequestBody StreamModels.CreateStreamInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.start(userId(jwt), input));
    }

    @PostMapping("/streams/{streamId}/end")
    StreamModels.StreamView end(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID streamId) {
        return service.end(userId(jwt), streamId);
    }

    @PostMapping("/streams/{streamId}/join")
    StreamModels.StreamSession join(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID streamId) {
        return service.join(userId(jwt), streamId);
    }

    @PostMapping("/streams/{streamId}/leave")
    ResponseEntity<Void> leave(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID streamId) {
        service.leave(userId(jwt), streamId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/streams/{streamId}/rtc-token")
    StreamModels.RtcCredentials token(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID streamId) {
        return service.token(userId(jwt), streamId);
    }

    @PutMapping("/streams/{streamId}/like")
    StreamModels.LikeView like(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID streamId) {
        return service.like(userId(jwt), streamId);
    }

    @DeleteMapping("/streams/{streamId}/like")
    StreamModels.LikeView unlike(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID streamId) {
        return service.unlike(userId(jwt), streamId);
    }

    @PostMapping("/streams/{streamId}/join-requests")
    ResponseEntity<StreamModels.JoinRequestView> requestCohost(@AuthenticationPrincipal Jwt jwt,
                                                               @PathVariable UUID streamId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.requestCohost(userId(jwt), streamId));
    }

    @GetMapping("/streams/{streamId}/join-requests")
    List<StreamModels.JoinRequestView> joinRequests(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable UUID streamId) {
        return service.joinRequests(userId(jwt), streamId);
    }

    @PatchMapping("/streams/{streamId}/join-requests/{requestId}")
    StreamModels.JoinRequestView decideRequest(@AuthenticationPrincipal Jwt jwt,
                                               @PathVariable UUID streamId,
                                               @PathVariable UUID requestId,
                                               @Valid @RequestBody StreamModels.RequestDecisionInput input) {
        return service.decideRequest(userId(jwt), streamId, requestId, input.status());
    }

    @PostMapping("/streams/{streamId}/invitations")
    ResponseEntity<StreamModels.InvitationView> invite(@AuthenticationPrincipal Jwt jwt,
                                                       @PathVariable UUID streamId,
                                                       @Valid @RequestBody StreamModels.InvitationInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.invite(userId(jwt), streamId, input.inviteeId()));
    }

    @GetMapping("/stream-invitations")
    List<StreamModels.InvitationView> invitations(@AuthenticationPrincipal Jwt jwt,
                                                  @RequestParam(defaultValue = "PENDING") String status) {
        return service.invitations(userId(jwt), status);
    }

    @PatchMapping("/stream-invitations/{invitationId}")
    StreamModels.InvitationView decideInvitation(@AuthenticationPrincipal Jwt jwt,
                                                 @PathVariable UUID invitationId,
                                                 @Valid @RequestBody StreamModels.InvitationDecisionInput input) {
        return service.decideInvitation(userId(jwt), invitationId, input.status());
    }

    @DeleteMapping("/streams/{streamId}/cohosts/{userId}")
    ResponseEntity<Void> removeCohost(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable UUID streamId, @PathVariable UUID userId) {
        service.removeCohost(userId(jwt), streamId, userId);
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
