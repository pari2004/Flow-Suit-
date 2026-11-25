package com.flowsuite.api.domain.enums;

/**
 * Organization membership roles, ordered from most to least privileged.
 *
 * <ul>
 *   <li>{@code OWNER}   — full control; manages subscription/billing, members, roles,
 *                         settings; the only role that can delete the org.</li>
 *   <li>{@code ADMIN}   — manages users, projects, customers, tasks; views analytics
 *                         and audit logs; cannot manage billing or delete the org.</li>
 *   <li>{@code MANAGER} — creates/manages projects &amp; customers, creates/assigns tasks
 *                         to org members, views team activity.</li>
 *   <li>{@code MEMBER}  — views only assigned projects/tasks and updates the status of
 *                         their own assigned tasks.</li>
 * </ul>
 */
public enum Role {
    OWNER,
    ADMIN,
    MANAGER,
    MEMBER
}
