package com.flowsuite.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for {@code flowsuite.jwt.*}. The secret must be at least 32 bytes
 * (256 bits) for HS256 signing.
 */
@ConfigurationProperties(prefix = "flowsuite.jwt")
public record JwtProperties(
        String secret,
        long accessTokenTtlMinutes,
        long refreshTokenTtlDays,
        long passwordResetTtlMinutes,
        String issuer
) {
}
