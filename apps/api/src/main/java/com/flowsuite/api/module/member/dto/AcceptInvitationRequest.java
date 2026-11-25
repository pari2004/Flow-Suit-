package com.flowsuite.api.module.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** An unregistered invitee registering into the org via their invite token (FR-11). */
public record AcceptInvitationRequest(
        @NotBlank String token,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(min = 8, max = 100) String password
) {
}
