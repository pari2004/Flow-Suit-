package com.flowsuite.api.module.auth;

import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import com.flowsuite.api.common.mail.EmailService;
import com.flowsuite.api.common.util.SlugGenerator;
import com.flowsuite.api.common.util.TokenHasher;
import com.flowsuite.api.config.JwtProperties;
import com.flowsuite.api.domain.entity.Membership;
import com.flowsuite.api.domain.entity.Organization;
import com.flowsuite.api.domain.entity.PasswordResetToken;
import com.flowsuite.api.domain.entity.Plan;
import com.flowsuite.api.domain.entity.RefreshToken;
import com.flowsuite.api.domain.entity.Subscription;
import com.flowsuite.api.domain.entity.User;
import com.flowsuite.api.domain.enums.MembershipStatus;
import com.flowsuite.api.domain.enums.PlanCode;
import com.flowsuite.api.domain.enums.Role;
import com.flowsuite.api.domain.enums.SubscriptionStatus;
import com.flowsuite.api.module.auth.dto.AuthResponse;
import com.flowsuite.api.module.auth.dto.ForgotPasswordRequest;
import com.flowsuite.api.module.auth.dto.LoginRequest;
import com.flowsuite.api.module.auth.dto.RegisterRequest;
import com.flowsuite.api.module.auth.dto.ResetPasswordRequest;
import com.flowsuite.api.module.auth.dto.UserSummary;
import com.flowsuite.api.repository.MembershipRepository;
import com.flowsuite.api.repository.OrganizationRepository;
import com.flowsuite.api.repository.PasswordResetTokenRepository;
import com.flowsuite.api.repository.PlanRepository;
import com.flowsuite.api.repository.RefreshTokenRepository;
import com.flowsuite.api.repository.SubscriptionRepository;
import com.flowsuite.api.repository.UserRepository;
import com.flowsuite.api.security.LoginRateLimiter;
import com.flowsuite.api.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Authentication + account lifecycle (FR-01..FR-06). Registration bootstraps the whole
 * tenant: a new organization, the user as its Owner, and a Free-plan subscription — all in
 * one transaction.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter loginRateLimiter;
    private final JwtProperties jwtProperties;
    private final AuthTokenIssuer tokenIssuer;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       MembershipRepository membershipRepository,
                       SubscriptionRepository subscriptionRepository,
                       PlanRepository planRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       LoginRateLimiter loginRateLimiter,
                       JwtProperties jwtProperties,
                       AuthTokenIssuer tokenIssuer,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginRateLimiter = loginRateLimiter;
        this.jwtProperties = jwtProperties;
        this.tokenIssuer = tokenIssuer;
        this.emailService = emailService;
    }

    /** FR-01/FR-02: register + auto-create organization with the user as Owner on the Free plan. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Plan freePlan = planRepository.findByCode(PlanCode.FREE)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "Free plan is not configured; run the plan seed migration."));

        String orgName = (request.organizationName() != null && !request.organizationName().isBlank())
                ? request.organizationName().trim()
                : request.name().trim() + "'s Organization";
        Organization organization = new Organization(
                orgName,
                SlugGenerator.toUniqueSlug(orgName, organizationRepository::existsBySlug));
        organizationRepository.save(organization);

        User user = new User();
        user.setEmail(email);
        user.setName(request.name().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        Membership membership = new Membership();
        membership.setOrganization(organization);
        membership.setUser(user);
        membership.setRole(Role.OWNER);
        membership.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(membership);

        Subscription subscription = new Subscription();
        subscription.setOrganization(organization);
        subscription.setPlan(freePlan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(Instant.now());
        subscriptionRepository.save(subscription);

        return tokenIssuer.issue(user, membership);
    }

    /** FR-03/FR-06: login with rate limiting on failed attempts. */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        loginRateLimiter.assertNotLocked(email);

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().getPasswordHash())) {
            loginRateLimiter.recordFailure(email);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        User user = userOpt.get();
        loginRateLimiter.reset(email);

        Membership membership = requireActiveMembership(user.getId());
        return tokenIssuer.issue(user, membership);
    }

    /** FR-04: rotate the refresh token and mint a fresh access token. */
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(TokenHasher.hash(rawRefreshToken))
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));
        if (!existing.isActive()) {
            throw new ApiException(ErrorCode.TOKEN_INVALID, "Refresh token is expired or revoked.");
        }

        // Rotation: revoke the presented token so it cannot be reused.
        existing.setRevokedAt(Instant.now());

        User user = existing.getUser();
        Membership membership = requireActiveMembership(user.getId());
        return tokenIssuer.issue(user, membership);
    }

    /** Revoke a single refresh token (logout on this device). Idempotent. */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(TokenHasher.hash(rawRefreshToken))
                .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    /**
     * FR-05: issue a time-limited reset token. Always returns normally — whether or not the
     * email exists — to avoid account enumeration.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            passwordResetTokenRepository.invalidateOutstandingForUser(user.getId(), Instant.now());

            String rawToken = TokenHasher.generateToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(TokenHasher.hash(rawToken));
            token.setExpiresAt(Instant.now().plus(jwtProperties.passwordResetTtlMinutes(), ChronoUnit.MINUTES));
            passwordResetTokenRepository.save(token);

            emailService.sendPasswordResetEmail(email, rawToken);
        });
    }

    /** FR-05: complete the reset; invalidates the token and all existing sessions. */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(TokenHasher.hash(request.token()))
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));
        if (!token.isUsable()) {
            throw new ApiException(ErrorCode.TOKEN_INVALID, "Reset token is expired or already used.");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.setUsedAt(Instant.now());
        // Force re-authentication everywhere after a password change.
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
    }

    /** Identity + tenant context for the authenticated caller (GET /auth/me). */
    @Transactional(readOnly = true)
    public UserSummary getCurrentUser() {
        UUID userId = SecurityUtils.currentUserId();
        return UserSummary.from(requireActiveMembership(userId));
    }

    // ---------------------------------------------------------------------

    private Membership requireActiveMembership(UUID userId) {
        Membership membership = membershipRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED, "No organization membership found."));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Your membership is not active.");
        }
        return membership;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
