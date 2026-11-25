package com.flowsuite.api.domain.enums;

/** Lifecycle of a user's membership within an organization. */
public enum MembershipStatus {
    /** Active member with the assigned role. */
    ACTIVE,
    /** Invited but not yet accepted/registered. */
    INVITED,
    /** Temporarily suspended by an owner/admin; retains the row but cannot act. */
    SUSPENDED,
    /** Removed from the org; retained for audit history. */
    REMOVED
}
