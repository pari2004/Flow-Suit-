package com.flowsuite.api.module.member.dto;

import com.flowsuite.api.domain.entity.Membership;
import com.flowsuite.api.domain.enums.MembershipStatus;
import com.flowsuite.api.domain.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
        UUID membershipId,
        UUID userId,
        String name,
        String email,
        Role role,
        MembershipStatus status,
        Instant joinedAt
) {
    public static MemberResponse from(Membership m) {
        return new MemberResponse(
                m.getId(), m.getUser().getId(), m.getUser().getName(), m.getUser().getEmail(),
                m.getRole(), m.getStatus(), m.getCreatedAt());
    }
}
