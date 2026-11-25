package com.flowsuite.api.module.auth;

import com.flowsuite.api.common.util.TokenHasher;
import com.flowsuite.api.config.JwtProperties;
import com.flowsuite.api.domain.entity.Membership;
import com.flowsuite.api.domain.entity.RefreshToken;
import com.flowsuite.api.domain.entity.User;
import com.flowsuite.api.module.auth.dto.AuthResponse;
import com.flowsuite.api.module.auth.dto.UserSummary;
import com.flowsuite.api.repository.RefreshTokenRepository;
import com.flowsuite.api.security.JwtService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Mints an access token + a persisted (rotated) refresh token for a user and returns the
 * standard {@link AuthResponse}. Shared by login/register and invitation acceptance.
 */
@Component
public class AuthTokenIssuer {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public AuthTokenIssuer(JwtService jwtService,
                           RefreshTokenRepository refreshTokenRepository,
                           JwtProperties jwtProperties) {
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    public AuthResponse issue(User user, Membership membership) {
        String accessToken = jwtService.generateAccessToken(user);

        String rawRefreshToken = TokenHasher.generateToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(TokenHasher.hash(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plus(jwtProperties.refreshTokenTtlDays(), ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.of(accessToken, rawRefreshToken,
                jwtService.accessTokenTtlSeconds(), UserSummary.from(membership));
    }
}
