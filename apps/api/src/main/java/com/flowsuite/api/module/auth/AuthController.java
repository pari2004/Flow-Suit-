package com.flowsuite.api.module.auth;

import com.flowsuite.api.common.dto.MessageResponse;
import com.flowsuite.api.module.auth.dto.AuthResponse;
import com.flowsuite.api.module.auth.dto.ForgotPasswordRequest;
import com.flowsuite.api.module.auth.dto.LoginRequest;
import com.flowsuite.api.module.auth.dto.RefreshTokenRequest;
import com.flowsuite.api.module.auth.dto.RegisterRequest;
import com.flowsuite.api.module.auth.dto.ResetPasswordRequest;
import com.flowsuite.api.module.auth.dto.UserSummary;
import com.flowsuite.api.module.member.MemberService;
import com.flowsuite.api.module.member.dto.AcceptInvitationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Authentication endpoints (FR-01..FR-06). All paths here are public except {@code /me}. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Registration, login, token refresh, and password reset")
public class AuthController {

    private static final String GENERIC_RESET_MESSAGE =
            "If an account exists for that email, a password reset link has been sent.";

    private final AuthService authService;
    private final MemberService memberService;

    public AuthController(AuthService authService, MemberService memberService) {
        this.authService = authService;
        this.memberService = memberService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register and auto-create an organization (user becomes Owner on Free plan)")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive access + refresh tokens")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new access token (rotates the refresh token)")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke a refresh token")
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset link")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(new MessageResponse(GENERIC_RESET_MESSAGE));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Complete a password reset using the emailed token")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Your password has been reset. Please log in again."));
    }

    @PostMapping("/accept-invitation")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Accept an invitation and register into the inviting organization")
    public AuthResponse acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request) {
        return memberService.acceptInvitation(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Current authenticated user + tenant context")
    public UserSummary me() {
        return authService.getCurrentUser();
    }
}
