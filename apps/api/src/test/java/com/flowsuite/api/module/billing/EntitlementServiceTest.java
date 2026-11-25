package com.flowsuite.api.module.billing;

import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import com.flowsuite.api.domain.entity.Organization;
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
import com.flowsuite.api.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntitlementServiceTest {

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock PlanRepository planRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock ProjectRepository projectRepository;

    @InjectMocks EntitlementService entitlementService;

    Organization org;
    UUID orgId;

    @BeforeEach
    void setUp() {
        org = TestFixtures.organization("Acme");
        orgId = org.getId();
    }

    private void subscribedTo(Plan plan, SubscriptionStatus status) {
        Subscription subscription = TestFixtures.subscription(org, plan, status);
        when(subscriptionRepository.findByOrganization_Id(orgId)).thenReturn(Optional.of(subscription));
    }

    @Test
    void hasEntitlement_readsNamedFlagFromEffectivePlan() {
        subscribedTo(TestFixtures.professionalPlan(), SubscriptionStatus.ACTIVE);
        assertThat(entitlementService.hasEntitlement(orgId, EntitlementService.ADVANCED_ANALYTICS)).isTrue();
    }

    @Test
    void freePlan_doesNotGrantAdvancedAnalytics() {
        subscribedTo(TestFixtures.freePlan(), SubscriptionStatus.ACTIVE);
        assertThat(entitlementService.hasEntitlement(orgId, EntitlementService.ADVANCED_ANALYTICS)).isFalse();
    }

    @Test
    void expiredSubscription_fallsBackToFreeEntitlements() {
        Plan free = TestFixtures.freePlan();
        subscribedTo(TestFixtures.professionalPlan(), SubscriptionStatus.EXPIRED);
        when(planRepository.findByCode(PlanCode.FREE)).thenReturn(Optional.of(free));

        Plan effective = entitlementService.effectivePlan(orgId);
        assertThat(effective.getCode()).isEqualTo(PlanCode.FREE);
    }

    @Test
    void assertCanAddProject_throwsWhenLimitReached() {
        subscribedTo(TestFixtures.freePlan(), SubscriptionStatus.ACTIVE); // limit 2
        when(projectRepository.countByOrganization_IdAndStatus(orgId, ProjectStatus.ACTIVE)).thenReturn(2L);

        assertThatThrownBy(() -> entitlementService.assertCanAddProject(orgId))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.PLAN_LIMIT_REACHED))
                .hasMessageContaining("Upgrade to Starter");
    }

    @Test
    void assertCanAddProject_allowsWhenUnderLimit() {
        subscribedTo(TestFixtures.freePlan(), SubscriptionStatus.ACTIVE);
        when(projectRepository.countByOrganization_IdAndStatus(orgId, ProjectStatus.ACTIVE)).thenReturn(1L);
        entitlementService.assertCanAddProject(orgId); // no throw
    }

    @Test
    void assertCanAddProject_unlimitedPlanNeverThrows() {
        subscribedTo(TestFixtures.professionalPlan(), SubscriptionStatus.ACTIVE); // unlimited projects
        entitlementService.assertCanAddProject(orgId); // no count lookup, no throw
    }

    @Test
    void assertCanAddMember_throwsAtSeatLimit() {
        subscribedTo(TestFixtures.freePlan(), SubscriptionStatus.ACTIVE); // 3 seats
        lenient().when(membershipRepository.countByOrganization_IdAndStatus(orgId, MembershipStatus.ACTIVE))
                .thenReturn(3L);

        assertThatThrownBy(() -> entitlementService.assertCanAddMember(orgId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("seats");
    }
}
