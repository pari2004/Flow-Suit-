package com.flowsuite.api.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Login with email + password (FR-03). */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
