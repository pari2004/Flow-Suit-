package com.flowsuite.api.config;

import com.flowsuite.api.module.usage.ApiUsageInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the API usage meter across tenant endpoints. Auth endpoints (unauthenticated)
 * and Stripe webhooks (not tenant-scoped) are excluded so they never consume quota.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiUsageInterceptor apiUsageInterceptor;

    public WebMvcConfig(ApiUsageInterceptor apiUsageInterceptor) {
        this.apiUsageInterceptor = apiUsageInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiUsageInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/auth/**", "/api/v1/webhooks/**");
    }
}
