package com.flowsuite.api.module.usage.dto;

import com.flowsuite.api.domain.enums.PlanCode;
import com.flowsuite.api.domain.enums.SubscriptionStatus;

/** Usage dashboard payload: current plan + usage-vs-limit for each metered resource (FR-50). */
public record UsageDashboardResponse(
        PlanCode planCode,
        String planName,
        SubscriptionStatus subscriptionStatus,
        Metric seats,
        Metric projects,
        Metric apiRequests,
        boolean advancedAnalytics
) {
    /** A used/limit pair. {@code limit == null} (and {@code unlimited == true}) means no cap. */
    public record Metric(long used, Long limit, boolean unlimited) {
        public static Metric of(long used, Integer limit) {
            return limit == null
                    ? new Metric(used, null, true)
                    : new Metric(used, limit.longValue(), false);
        }
    }
}
