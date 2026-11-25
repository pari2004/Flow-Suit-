package com.flowsuite.api.security;

import com.flowsuite.api.domain.enums.Role;

import java.util.UUID;

/**
 * The authenticated caller's resolved identity for the current request — the output of the
 * first three steps of the authorization check:
 * <ol>
 *   <li>who the user is ({@code userId}),</li>
 *   <li>which organization they act within ({@code organizationId}),</li>
 *   <li>confirmed via an active membership (this object only exists if that held),</li>
 * </ol>
 * and {@code role}, used for the fourth step (does the role permit the action).
 *
 * <p>Stored as the Spring Security {@code Authentication} principal. Controllers/services
 * read it via {@link SecurityUtils} and <b>must</b> scope every query to
 * {@link #organizationId()}.
 */
public record AuthPrincipal(
        UUID userId,
        UUID organizationId,
        Role role,
        String email
) {
}
