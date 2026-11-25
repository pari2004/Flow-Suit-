package com.flowsuite.api.security;

import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import com.flowsuite.api.config.JwtProperties;
import com.flowsuite.api.domain.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates short-lived JWT <b>access</b> tokens (HS256). Refresh tokens are
 * opaque random strings handled separately (see {@code AuthService}); the access token
 * carries only the user id (subject) and email — the organization and role are resolved
 * fresh from the database on each request so role/membership changes take effect
 * immediately rather than waiting for the token to expire.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final String issuer;
    private final long accessTokenTtlMinutes;

    public JwtService(JwtProperties properties) {
        byte[] keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "flowsuite.jwt.secret must be at least 32 bytes for HS256 signing");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = properties.issuer();
        this.accessTokenTtlMinutes = properties.accessTokenTtlMinutes();
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtlMinutes * 60;
    }

    /**
     * Validates the signature + expiry and returns the subject (user id). Throws an
     * {@link ApiException} with a precise code the client can distinguish.
     */
    public UUID parseUserId(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return UUID.fromString(subject);
        } catch (ExpiredJwtException e) {
            throw new ApiException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        }
    }
}
