package com.livestream.platform.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final OtpService otpService;
    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(OtpService otpService, AuthService authService, TokenService tokenService) {
        this.otpService = otpService;
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/otp/request")
    ResponseEntity<AuthModels.OtpResponse> requestOtp(@Valid @RequestBody AuthModels.OtpInput input,
                                                       HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(otpService.request(input, ip(request)));
    }

    @PostMapping("/register/phone")
    ResponseEntity<AuthModels.TokenResponse> registerPhone(
            @Valid @RequestBody AuthModels.PhoneRegistrationInput input, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerPhone(input, ip(request), userAgent(request)));
    }

    @PostMapping("/login/phone")
    AuthModels.TokenResponse loginPhone(@Valid @RequestBody AuthModels.PhoneLoginInput input,
                                        HttpServletRequest request) {
        return authService.loginPhone(input, ip(request), userAgent(request));
    }

    @PostMapping("/register/email")
    ResponseEntity<AuthModels.TokenResponse> registerEmail(
            @Valid @RequestBody AuthModels.EmailRegistrationInput input, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerEmail(input, ip(request), userAgent(request)));
    }

    @PostMapping("/login/email")
    AuthModels.TokenResponse loginEmail(@Valid @RequestBody AuthModels.EmailLoginInput input,
                                        HttpServletRequest request) {
        return authService.loginEmail(input, ip(request), userAgent(request));
    }

    @PostMapping("/password/reset")
    ResponseEntity<Void> resetPassword(@Valid @RequestBody AuthModels.PasswordResetInput input) {
        authService.resetPassword(input);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/token/refresh")
    AuthModels.TokenResponse refresh(@Valid @RequestBody AuthModels.RefreshInput input,
                                     HttpServletRequest request) {
        return authService.refresh(input.refreshToken(), ip(request), userAgent(request));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt,
                                @Valid @RequestBody AuthModels.RefreshInput input) {
        tokenService.revoke(input.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    ResponseEntity<Void> logoutAll(@AuthenticationPrincipal Jwt jwt) {
        tokenService.revokeAll(userId(jwt));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    List<AuthModels.SessionResponse> sessions(@AuthenticationPrincipal Jwt jwt) {
        return authService.sessions(userId(jwt));
    }

    @DeleteMapping("/sessions/{deviceId}")
    ResponseEntity<Void> revokeSession(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID deviceId) {
        tokenService.revokeDevice(userId(jwt), deviceId);
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private String ip(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
