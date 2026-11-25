package com.flowsuite.api.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration (FR-01). Creates the user, auto-creates their organization, and makes them
 * its Owner on the Free plan. {@code organizationName} is optional — if blank, a name is
 * derived from the user's name.
 */
public record RegisterRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Size(max = 255) String organizationName
) {
}
