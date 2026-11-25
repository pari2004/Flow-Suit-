package com.flowsuite.api.common.audit;

/** Stable audit action codes. Centralized so logs stay queryable/filterable (FR-51). */
public final class AuditAction {

    private AuditAction() {
    }

    // Membership / roles
    public static final String MEMBER_INVITED = "MEMBER_INVITED";
    public static final String INVITATION_REVOKED = "INVITATION_REVOKED";
    public static final String MEMBER_JOINED = "MEMBER_JOINED";
    public static final String MEMBER_ROLE_CHANGED = "MEMBER_ROLE_CHANGED";
    public static final String MEMBER_REMOVED = "MEMBER_REMOVED";

    // Organization
    public static final String ORGANIZATION_UPDATED = "ORGANIZATION_UPDATED";
    public static final String ORGANIZATION_DELETED = "ORGANIZATION_DELETED";

    // Billing / subscription
    public static final String CHECKOUT_STARTED = "CHECKOUT_STARTED";
    public static final String SUBSCRIPTION_UPDATED = "SUBSCRIPTION_UPDATED";
    public static final String SUBSCRIPTION_CANCELLED = "SUBSCRIPTION_CANCELLED";
}
