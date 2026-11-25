package com.flowsuite.api.domain.entity;

import com.flowsuite.api.domain.enums.MembershipStatus;
import com.flowsuite.api.domain.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Links a {@link User} to their {@link Organization} and carries their {@link Role}.
 *
 * <p>Tenant-owned. A unique constraint on {@code user_id} enforces the "one user, one
 * organization" rule; {@code (organization_id, user_id)} is additionally unique.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "memberships")
public class Membership extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MembershipStatus status = MembershipStatus.ACTIVE;
}
