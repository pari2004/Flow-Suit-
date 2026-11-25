package com.flowsuite.api.security;

import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Convenience accessors for the authenticated {@link AuthPrincipal}. Service code uses
 * {@link #currentOrganizationId()} to scope every tenant query — this is the enforcement
 * point for strict tenant isolation.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        return principal;
    }

    public static UUID currentUserId() {
        return currentPrincipal().userId();
    }

    /** The organization every query in the current request MUST be filtered by. */
    public static UUID currentOrganizationId() {
        return currentPrincipal().organizationId();
    }
}
