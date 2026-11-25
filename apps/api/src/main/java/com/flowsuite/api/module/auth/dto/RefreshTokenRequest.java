package com.flowsuite.api.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Exchange a refresh token for a new access token (FR-04). Also used for logout. */
public record RefreshTokenRequest(
        @NotBlank String refreshToken
) {
}
