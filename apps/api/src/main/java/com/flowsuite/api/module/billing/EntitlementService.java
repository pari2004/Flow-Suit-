package com.flowsuite.api.module.billing;

import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import com.flowsuite.api.domain.entity.Plan;
import com.flowsuite.api.domain.entity.Subscription;
import com.flowsuite.api.domain.enums.MembershipStatus;
import com.flowsuite.api.domain.enums.PlanCode;
import com.flowsuite.api.domain.enums.ProjectStatus;
import com.flowsuite.api.domain.enums.SubscriptionStatus;
import com.flowsuite.api.repository.MembershipRepository;
import com.flowsuite.api.repository.PlanRepository;
import com.flowsuite.api.repository.ProjectRepository;
import com.flowsuite.api.repository.SubscriptionRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Central place for plan limits and named entitlement checks (FR-42..FR-45).
 *
 * <p>Decisions are driven by the org's <b>effective plan</b> — its subscribed plan while the
 * subscription grants access (ACTIVE/TRIALING, or PAST_DUE still within its grace period),
 * otherwise a downgrade to Free. Callers ask by entitlement name
 * ({@link #hasEntitlement}) or assert a limit ({@code assertCanAdd*}); nothing branches on
 * plan-name strings.
 */
@Service
public class EntitlementService {

    public static final String ADVANCED_ANALYTICS = "ADVANCED_ANALYTICS";

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final MembershipRepository membershipRepository;
    private final ProjectRepository projectRepository;

    public EntitlementService(SubscriptionRepository subscriptionRepository,
                              PlanRepository planRepository,
                              MembershipRepository membershipRepository,
                              ProjectRepository projectRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.membershipRepository = membershipRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public Subscription currentSubscription(UUID organizationId) {
        return subscriptionRepository.findByOrganization_Id(organizationId)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No subscription found for organization."));
    }

    /**
     * The plan whose limits/entitlements currently apply to the org. The returned entity is
     * fully initialized (not a lazy proxy) so callers may read it outside this transaction —
     * e.g. the usage interceptor, which runs with no active session.
     */
    @Transactional(readOnly = true)
    public Plan effectivePlan(UUID organizationId) {
        Subscription subscription = currentSubscription(organizationId);
        Plan plan = grantsAccess(subscription)
                ? subscription.getPlan()
                : planRepository.findByCode(PlanCode.FREE)
                        .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR, "Free plan is not configured."));
        Hibernate.initialize(plan);
        return plan;
    }

    @Transactional(readOnly = true)
    public boolean hasEntitlement(UUID organizationId, String entitlement) {
        return effectivePlan(organizationId).hasEntitlement(entitlement);
    }

    /** Guards a feature behind a named entitlement, throwing an actionable 403 if absent. */
    @Transactional(readOnly = true)
    public void requireEntitlement(UUID organizationId, String entitlement) {
        if (!hasEntitlement(organizationId, entitlement)) {
            throw new ApiException(ErrorCode.PLAN_LIMIT_REACHED,
                    "This feature (" + entitlement + ") is not included in your current plan. "
                            + "Upgrade to Professional to unlock it.");
        }
    }

    /** Seat-limit enforcement before adding/inviting a member (FR-45). */
    @Transactional(readOnly = true)
    public void assertCanAddMember(UUID organizationId) {
        Plan plan = effectivePlan(organizationId);
        long activeSeats = membershipRepository
                .countByOrganization_IdAndStatus(organizationId, MembershipStatus.ACTIVE);
        if (activeSeats >= plan.getSeatLimit()) {
            throw new ApiException(ErrorCode.PLAN_LIMIT_REACHED, String.format(
                    "You've reached your plan's limit of %d seats. %s",
                    plan.getSeatLimit(), upgradeHint(plan, "seats")));
        }
    }

    /** Project-limit enforcement before creating a project (FR-20, FR-44). */
    @Transactional(readOnly = true)
    public void assertCanAddProject(UUID organizationId) {
        Plan plan = effectivePlan(organizationId);
        if (plan.hasUnlimitedProjects()) {
            return;
        }
        long activeProjects = projectRepository
                .countByOrganization_IdAndStatus(organizationId, ProjectStatus.ACTIVE);
        if (activeProjects >= plan.getProjectLimit()) {
            throw new ApiException(ErrorCode.PLAN_LIMIT_REACHED, String.format(
                    "You've reached your plan's limit of %d projects. %s",
                    plan.getProjectLimit(), upgradeHint(plan, "projects")));
        }
    }

    private boolean grantsAccess(Subscription subscription) {
        SubscriptionStatus status = subscription.getStatus();
        return switch (status) {
            case ACTIVE, TRIALING -> true;
            case PAST_DUE -> subscription.getGracePeriodEndsAt() == null
                    || subscription.getGracePeriodEndsAt().isAfter(Instant.now());
            case CANCELLED, EXPIRED -> false;
        };
    }

    /** Suggests the next plan up that raises the given limit. */
    private String upgradeHint(Plan currentPlan, String resource) {
        PlanCode next = switch (currentPlan.getCode()) {
            case FREE -> PlanCode.STARTER;
            case STARTER -> PlanCode.PROFESSIONAL;
            case PROFESSIONAL -> null;
        };
        if (next == null) {
            return "You are on the highest plan.";
        }
        return "Upgrade to " + capitalize(next.name()) + " for more " + resource + ".";
    }

    private static String capitalize(String value) {
        return value.charAt(0) + value.substring(1).toLowerCase();
    }
}
