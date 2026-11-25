package com.flowsuite.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for {@code flowsuite.mail.*}.
 *
 * <p>Controls outbound transactional email (password-reset and invitation links). SMTP
 * transport itself is configured via the standard {@code spring.mail.*} properties; this
 * record only carries FlowSuite-level concerns: the sender identity, the public app base
 * URL used to build links, and a master switch.
 */
@ConfigurationProperties(prefix = "flowsuite.mail")
public record MailProperties(
        String fromAddress,
        String fromName,
        String appBaseUrl,
        Boolean enabled
) {
    public MailProperties {
        if (fromAddress == null || fromAddress.isBlank()) {
            fromAddress = "no-reply@flowsuite.local";
        }
        if (fromName == null || fromName.isBlank()) {
            fromName = "FlowSuite";
        }
        if (appBaseUrl == null || appBaseUrl.isBlank()) {
            appBaseUrl = "http://localhost:5173";
        }
        if (enabled == null) {
            enabled = Boolean.TRUE;
        }
    }

    /** Base URL without a trailing slash, for safe link concatenation. */
    public String normalizedBaseUrl() {
        return appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
    }
}
