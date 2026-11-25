package com.flowsuite.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A global user identity. Email is unique across the whole system. A user belongs to
 * exactly one organization via a {@link Membership} row (enforced by a unique constraint
 * on {@code memberships.user_id}).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "name", nullable = false)
    private String name;

    /** BCrypt hash — never the raw password. Nullable only transiently before a reset. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;
}
