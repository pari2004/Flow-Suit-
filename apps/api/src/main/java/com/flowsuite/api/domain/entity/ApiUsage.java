package com.flowsuite.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Persisted per-organization API request count for a billing period (FR-45, FR-50).
 *
 * <p>Redis is the live source of truth during the month (fast atomic increments); this
 * table persists the count per period so the usage dashboard can show history and the
 * scheduled reset job has a durable record. Unique on {@code (organization_id, period_start)}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "api_usage")
public class ApiUsage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** First day of the billing month this counter covers. */
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "request_count", nullable = false)
    private long requestCount = 0;
}
