package com.flowsuite.api.domain.entity;

import com.flowsuite.api.domain.enums.PlanCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * A subscription plan. Plans are <b>configuration data</b> stored in the database (seeded
 * via a repeatable Flyway migration), never hardcoded in application logic (FR-40).
 *
 * <p>Limits and entitlements live here so code can ask {@code plan.hasEntitlement(...)}
 * and compare against numeric limits rather than branching on the plan name.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "plans")
public class Plan extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, unique = true, length = 32)
    private PlanCode code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Monthly price in Indian Rupees (whole rupees). */
    @Column(name = "price_inr", nullable = false)
    private int priceInr;

    @Column(name = "seat_limit", nullable = false)
    private int seatLimit;

    /** Maximum projects. {@code null} means unlimited (Professional). */
    @Column(name = "project_limit")
    private Integer projectLimit;

    @Column(name = "api_request_limit", nullable = false)
    private int apiRequestLimit;

    /** Named feature flags, e.g. {@code {"ADVANCED_ANALYTICS": true}}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entitlements", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> entitlements = new HashMap<>();

    /** Stripe (Test Mode) price id used at checkout; null for Free. */
    @Column(name = "stripe_price_id")
    private String stripePriceId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** True when the named entitlement flag is present and truthy. */
    public boolean hasEntitlement(String key) {
        Object value = entitlements.get(key);
        return value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
    }

    /** True when projects are unlimited on this plan. */
    public boolean hasUnlimitedProjects() {
        return projectLimit == null;
    }
}
