package com.flowsuite.api.domain.enums;

/** Lifecycle of an organization invitation (FR-10, FR-11). */
public enum InvitationStatus {
    /** Sent, awaiting the invitee to accept/register. */
    PENDING,
    /** Accepted — the invitee joined the org. */
    ACCEPTED,
    /** Expired past its time limit without being accepted. */
    EXPIRED,
    /** Revoked by an owner/admin before acceptance. */
    REVOKED
}
