package com.flowsuite.api.module.auth;

import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import com.flowsuite.api.config.JwtProperties;
import com.flowsuite.api.domain.entity.Membership;
import com.flowsuite.api.domain.entity.Organization;
import com.flowsuite.api.domain.entity.User;
import com.flowsuite.api.domain.enums.PlanCode;
import com.flowsuite.api.domain.enums.Role;
import com.flowsuite.api.module.auth.dto.AuthResponse;
import com.flowsuite.api.module.auth.dto.LoginRequest;
import com.flowsuite.api.module.auth.dto.RegisterRequest;
import com.flowsuite.api.module.auth.dto.UserSummary;
import com.flowsuite.api.repository.MembershipRepository;
import com.flowsuite.api.repository.OrganizationRepository;
import com.flowsuite.api.repository.PasswordResetTokenRepository;
import com.flowsuite.api.repository.PlanRepository;
import com.flowsuite.api.repository.RefreshTokenRepository;
import com.flowsuite.api.repository.SubscriptionRepository;
import com.flowsuite.api.repository.UserRepository;
import com.flowsuite.api.security.LoginRateLimiter;
import com.flowsuite.api.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock PlanRepository planRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock LoginRateLimiter loginRateLimiter;
    @Mock AuthTokenIssuer tokenIssuer;
    @Mock com.flowsuite.api.common.mail.EmailService emailService;

    AuthService authService() {
        JwtProperties props = new JwtProperties("secret-secret-secret-secret-secret-32b", 15, 30, 30, "flowsuite");
        return new AuthService(userRepository, organizationRepository, membershipRepository,
                subscriptionRepository, planRepository, refreshTokenRepository, passwordResetTokenRepository,
                passwordEncoder, loginRateLimiter, props, tokenIssuer, emailService);
    }

    private AuthResponse sampleResponse() {
        return AuthResponse.of("access", "refresh", 900,
                new UserSummary(null, "A", "a@b.com", null, "Org", Role.OWNER));
    }

    @Test
    void register_createsOrgOwnerAndFreeSubscription() {
        when(userRepository.existsByEmailIgnoreCase("a@b.com")).thenReturn(false);
        when(planRepository.findByCode(PlanCode.FREE)).thenReturn(Optional.of(TestFixtures.freePlan()));
        when(organizationRepository.existsBySlug(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(tokenIssuer.issue(any(), any())).thenReturn(sampleResponse());

        AuthResponse response = authService().register(
                new RegisterRequest("Alice", "A@B.com", "password123", null));

        assertThat(response.accessToken()).isEqualTo("access");
        verify(organizationRepository).save(any(Organization.class));
        verify(membershipRepository).save(any(Membership.class));
        verify(subscriptionRepository).save(any());
        // Owner role + Free plan wired through the issuer
        verify(tokenIssuer).issue(any(User.class), any(Membership.class));
    }

    @Test
    void register_rejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("a@b.com")).thenReturn(true);

        assertThatThrownBy(() -> authService().register(new RegisterRequest("A", "a@b.com", "password123", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_invalidPassword_recordsFailureAndThrows() {
        User user = TestFixtures.user("a@b.com", "A", "stored-hash");
        when(userRepository.findByEmailIgnoreCase("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService().login(new LoginRequest("a@b.com", "wrong")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
        verify(loginRateLimiter).recordFailure("a@b.com");
    }

    @Test
    void login_whenLockedOut_throwsRateLimited() {
        doThrow(new ApiException(ErrorCode.RATE_LIMITED)).when(loginRateLimiter).assertNotLocked("a@b.com");

        assertThatThrownBy(() -> authService().login(new LoginRequest("a@b.com", "whatever")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.RATE_LIMITED));
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }
}
