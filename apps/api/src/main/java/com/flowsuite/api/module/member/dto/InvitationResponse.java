package com.flowsuite.api.module.member.dto;

import com.flowsuite.api.domain.entity.Invitation;
import com.flowsuite.api.domain.enums.InvitationStatus;
import com.flowsuite.api.domain.enums.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * An invitation. {@code acceptToken} is the raw token and is included ONLY in the response
 * to the inviter who just created it, so the frontend can build the accept link; it is
 * never returned by the list endpoint.
 */
public record InvitationResponse(
        UUID id,
        String email,
        Role role,
        InvitationStatus status,
        Instant expiresAt,
        Instant createdAt,
        String acceptToken
) {
    public static InvitationResponse from(Invitation inv) {
        return new InvitationResponse(inv.getId(), inv.getEmail(), inv.getRole(), inv.getStatus(),
                inv.getExpiresAt(), inv.getCreatedAt(), null);
    }

    public static InvitationResponse withToken(Invitation inv, String rawToken) {
        return new InvitationResponse(inv.getId(), inv.getEmail(), inv.getRole(), inv.getStatus(),
                inv.getExpiresAt(), inv.getCreatedAt(), rawToken);
    }
}
