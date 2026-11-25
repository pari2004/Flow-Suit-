package com.flowsuite.api.module.auth.dto;

import com.flowsuite.api.domain.entity.Membership;
import com.flowsuite.api.domain.enums.Role;

import java.util.UUID;

/** The authenticated user's identity + tenant context, returned alongside tokens. */
public record UserSummary(
        UUID id,
        String name,
        String email,
        UUID organizationId,
        String organizationName,
        Role role
) {
    public static UserSummary from(Membership membership) {
        return new UserSummary(
                membership.getUser().getId(),
                membership.getUser().getName(),
                membership.getUser().getEmail(),
                membership.getOrganization().getId(),
                membership.getOrganization().getName(),
                membership.getRole());
    }
}
