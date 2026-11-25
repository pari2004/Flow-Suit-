package com.flowsuite.api.module.usage;

import com.flowsuite.api.security.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Meters authenticated, tenant-scoped API requests and enforces the monthly quota before
 * the controller runs. Only requests with a resolved {@link AuthPrincipal} are counted
 * (public auth/webhook/health routes are excluded by registration). A quota breach throws
 * {@code API_QUOTA_EXCEEDED}, which the global handler renders as a 429.
 */
@Component
public class ApiUsageInterceptor implements HandlerInterceptor {

    private final UsageService usageService;

    public ApiUsageInterceptor(UsageService usageService) {
        this.usageService = usageService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthPrincipal principal) {
            usageService.incrementAndEnforce(principal.organizationId());
        }
        return true;
    }
}
