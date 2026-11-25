package com.flowsuite.api.module.auth.dto;

/**
 * Successful auth result: a short-lived access token (JWT) + an opaque refresh token,
 * with the token type and access-token lifetime (seconds), plus the user/tenant summary.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, UserSummary user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
