package com.flowsuite.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed binding for {@code flowsuite.stripe.*} (Test Mode). */
@ConfigurationProperties(prefix = "flowsuite.stripe")
public record StripeProperties(
        String secretKey,
        String webhookSecret,
        String successUrl,
        String cancelUrl
) {
    /** True when a real (non-placeholder) secret key is configured. */
    public boolean isConfigured() {
        return secretKey != null && secretKey.startsWith("sk_") && !secretKey.equals("sk_test_xxx");
    }
}
