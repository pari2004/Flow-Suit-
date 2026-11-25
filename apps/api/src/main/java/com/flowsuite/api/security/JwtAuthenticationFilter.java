package com.flowsuite.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowsuite.api.common.error.ApiError;
import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import com.flowsuite.api.domain.entity.Membership;
import com.flowsuite.api.domain.enums.MembershipStatus;
import com.flowsuite.api.repository.MembershipRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the caller from the {@code Authorization: Bearer <access-token>} header and
 * establishes the per-request {@link AuthPrincipal}. Implements the first three steps of
 * the authorization check:
 * <ol>
 *   <li>validate the token → the user id,</li>
 *   <li>load the user's single active {@link Membership} → the organization,</li>
 *   <li>confirm the membership is {@code ACTIVE} (a removed/suspended user is rejected
 *       immediately, even with an otherwise-valid token).</li>
 * </ol>
 * The role is attached as a {@code ROLE_*} authority for the fourth step (method security).
 *
 * <p>Requests without a bearer token pass through unauthenticated; the security config then
 * rejects protected routes via the authentication entry point.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final MembershipRepository membershipRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   MembershipRepository membershipRepository,
                                   ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.membershipRepository = membershipRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();
        UUID userId;
        try {
            userId = jwtService.parseUserId(token);
        } catch (ApiException ex) {
            // Expired or malformed token — fail fast with the precise code.
            writeError(response, ex.getErrorCode(), ex.getMessage());
            return;
        }

        Optional<Membership> membershipOpt = membershipRepository.findByUser_Id(userId);
        if (membershipOpt.isEmpty() || membershipOpt.get().getStatus() != MembershipStatus.ACTIVE) {
            // Valid token but no active membership (removed/suspended) — treat as invalid session.
            writeError(response, ErrorCode.UNAUTHENTICATED, "Your session is no longer valid.");
            return;
        }

        Membership membership = membershipOpt.get();
        AuthPrincipal principal = new AuthPrincipal(
                membership.getUser().getId(),
                membership.getOrganization().getId(),
                membership.getRole(),
                membership.getUser().getEmail());

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + membership.getRole().name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, ErrorCode code, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError body = new ApiError(code.name(), message, code.status().value());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
