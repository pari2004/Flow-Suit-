package com.flowsuite.api.domain.entity;

import com.flowsuite.api.domain.enums.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * An organization's current subscription. One row per organization (unique
 * {@code organization_id}). The {@link Plan} association drives entitlement/limit checks;
 * Stripe identifiers tie it to the Test Mode billing object (Phase 3).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subscriptions")
public class Subscription extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "current_period_start")
    private Instant currentPeriodStart;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    /** When PAST_DUE, access is retained until this instant (grace period). */
    @Column(name = "grace_period_ends_at")
    private Instant gracePeriodEndsAt;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd = false;
}
