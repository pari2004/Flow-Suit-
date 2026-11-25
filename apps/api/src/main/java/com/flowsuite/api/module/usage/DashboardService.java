package com.flowsuite.api.module.usage;

import com.flowsuite.api.domain.entity.Plan;
import com.flowsuite.api.domain.entity.Subscription;
import com.flowsuite.api.domain.enums.MembershipStatus;
import com.flowsuite.api.domain.enums.ProjectStatus;
import com.flowsuite.api.module.billing.EntitlementService;
import com.flowsuite.api.module.usage.dto.UsageDashboardResponse;
import com.flowsuite.api.module.usage.dto.UsageDashboardResponse.Metric;
import com.flowsuite.api.repository.MembershipRepository;
import com.flowsuite.api.repository.ProjectRepository;
import com.flowsuite.api.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Aggregates current plan + usage-vs-limit for the usage dashboard (FR-50). */
@Service
public class DashboardService {

    private final EntitlementService entitlementService;
    private final MembershipRepository membershipRepository;
    private final ProjectRepository projectRepository;
    private final UsageService usageService;

    public DashboardService(EntitlementService entitlementService,
                            MembershipRepository membershipRepository,
                            ProjectRepository projectRepository,
                            UsageService usageService) {
        this.entitlementService = entitlementService;
        this.membershipRepository = membershipRepository;
        this.projectRepository = projectRepository;
        this.usageService = usageService;
    }

    @Transactional(readOnly = true)
    public UsageDashboardResponse getDashboard() {
        UUID organizationId = SecurityUtils.currentOrganizationId();
        Subscription subscription = entitlementService.currentSubscription(organizationId);
        Plan plan = entitlementService.effectivePlan(organizationId);

        long seatsUsed = membershipRepository
                .countByOrganization_IdAndStatus(organizationId, MembershipStatus.ACTIVE);
        long projectsUsed = projectRepository
                .countByOrganization_IdAndStatus(organizationId, ProjectStatus.ACTIVE);
        long apiUsed = usageService.currentUsage(organizationId);

        return new UsageDashboardResponse(
                plan.getCode(),
                plan.getName(),
                subscription.getStatus(),
                Metric.of(seatsUsed, plan.getSeatLimit()),
                Metric.of(projectsUsed, plan.getProjectLimit()),
                Metric.of(apiUsed, plan.getApiRequestLimit()),
                plan.hasEntitlement(EntitlementService.ADVANCED_ANALYTICS));
    }
}
