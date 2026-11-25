package com.flowsuite.api.domain.enums;

/**
 * Subscription lifecycle states (FR-41).
 *
 * <pre>
 *   TRIALING ──▶ ACTIVE ──▶ PAST_DUE ──▶ (ACTIVE | CANCELLED | EXPIRED)
 *                   │
 *                   └──────▶ CANCELLED ──▶ EXPIRED
 * </pre>
 */
public enum SubscriptionStatus {
    /** In a trial window; entitlements granted as if active. */
    TRIALING,
    /** Paid and current. */
    ACTIVE,
    /** Payment failed; within the grace period, access retained until it elapses. */
    PAST_DUE,
    /** Cancelled by the customer; access may continue until period end. */
    CANCELLED,
    /** Fully expired; downgraded to Free entitlements. */
    EXPIRED
}
