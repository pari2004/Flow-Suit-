package com.flowsuite.api.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request a password reset link (FR-05). Always answered generically to avoid enumeration. */
public record ForgotPasswordRequest(
        @NotBlank @Email String email
) {
}
