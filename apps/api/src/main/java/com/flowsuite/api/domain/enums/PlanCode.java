package com.flowsuite.api.domain.enums;

/**
 * Stable plan identifiers. These are the DB {@code plans.code} values — code never
 * branches on plan <em>name</em> strings; entitlement decisions use the named
 * entitlement flags/limits on the Plan row instead (FR-43).
 */
public enum PlanCode {
    FREE,
    STARTER,
    PROFESSIONAL
}
