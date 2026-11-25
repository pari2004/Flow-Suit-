package com.flowsuite.api.module.member.dto;

import com.flowsuite.api.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Invite someone by email with a role (FR-10). Owner cannot be assigned via invitation. */
public record InviteMemberRequest(
        @NotBlank @Email String email,
        @NotNull Role role
) {
}
