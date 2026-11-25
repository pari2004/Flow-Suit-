package com.flowsuite.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for {@code flowsuite.security.*} — login rate-limiting thresholds (FR-06).
 */
@ConfigurationProperties(prefix = "flowsuite.security")
public record SecurityProperties(
        int loginMaxAttempts,
        int loginWindowMinutes
) {
}
