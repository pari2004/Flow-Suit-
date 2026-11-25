package com.flowsuite.api.support;

import com.flowsuite.api.domain.enums.Role;
import com.flowsuite.api.security.AuthPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

/** Test helper to populate the SecurityContext with an {@link AuthPrincipal}. */
public final class SecurityContextSupport {

    private SecurityContextSupport() {
    }

    public static AuthPrincipal authenticate(UUID userId, UUID organizationId, Role role) {
        AuthPrincipal principal = new AuthPrincipal(userId, organizationId, role, "user@test.com");
        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
        return principal;
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
