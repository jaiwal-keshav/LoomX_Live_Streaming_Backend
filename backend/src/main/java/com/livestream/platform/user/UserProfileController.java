package com.livestream.platform.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {
    private final UserProfileService service;

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    @GetMapping("/me")
    ProfileModels.AccountView me(@AuthenticationPrincipal Jwt jwt) {
        return service.me(userId(jwt));
    }

    @PatchMapping("/me/profile")
    ProfileModels.ProfileView update(@AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody ProfileModels.UpdateProfileInput input) {
        return service.update(userId(jwt), input);
    }

    @GetMapping("/{username}")
    ProfileModels.ProfileView publicProfile(@PathVariable String username) {
        return service.publicProfile(username);
    }

    @DeleteMapping("/me")
    ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt) {
        service.delete(userId(jwt));
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
