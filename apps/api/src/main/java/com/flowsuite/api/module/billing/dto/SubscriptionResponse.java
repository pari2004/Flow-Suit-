package com.flowsuite.api.module.billing.dto;

import com.flowsuite.api.domain.entity.Subscription;
import com.flowsuite.api.domain.enums.PlanCode;
import com.flowsuite.api.domain.enums.SubscriptionStatus;

import java.time.Instant;

public record SubscriptionResponse(
        PlanCode planCode,
        String planName,
        SubscriptionStatus status,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        Instant trialEndsAt,
        Instant gracePeriodEndsAt,
        boolean cancelAtPeriodEnd
) {
    public static SubscriptionResponse from(Subscription s) {
        return new SubscriptionResponse(
                s.getPlan().getCode(), s.getPlan().getName(), s.getStatus(),
                s.getCurrentPeriodStart(), s.getCurrentPeriodEnd(), s.getTrialEndsAt(),
                s.getGracePeriodEndsAt(), s.isCancelAtPeriodEnd());
    }
}
