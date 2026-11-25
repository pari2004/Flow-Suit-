package com.flowsuite.api.support;

import com.flowsuite.api.domain.entity.Membership;
import com.flowsuite.api.domain.entity.Organization;
import com.flowsuite.api.domain.entity.Plan;
import com.flowsuite.api.domain.entity.Subscription;
import com.flowsuite.api.domain.entity.User;
import com.flowsuite.api.domain.enums.MembershipStatus;
import com.flowsuite.api.domain.enums.PlanCode;
import com.flowsuite.api.domain.enums.Role;
import com.flowsuite.api.domain.enums.SubscriptionStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;

/** Builders for unit tests — entities with ids assigned via reflection (no persistence). */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static Plan plan(PlanCode code, int seats, Integer projects, int api, boolean advancedAnalytics) {
        Plan plan = new Plan();
        plan.setCode(code);
        plan.setName(code.name());
        plan.setPriceInr(0);
        plan.setSeatLimit(seats);
        plan.setProjectLimit(projects);
        plan.setApiRequestLimit(api);
        plan.setEntitlements(Map.of("ADVANCED_ANALYTICS", advancedAnalytics));
        plan.setActive(true);
        setId(plan);
        return plan;
    }

    public static Plan freePlan() {
        return plan(PlanCode.FREE, 3, 2, 1000, false);
    }

    public static Plan professionalPlan() {
        return plan(PlanCode.PROFESSIONAL, 50, null, 100000, true);
    }

    public static Organization organization(String name) {
        Organization org = new Organization(name, name.toLowerCase().replace(' ', '-'));
        setId(org);
        return org;
    }

    public static User user(String email, String name, String passwordHash) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPasswordHash(passwordHash);
        setId(user);
        return user;
    }

    public static Subscription subscription(Organization org, Plan plan, SubscriptionStatus status) {
        Subscription subscription = new Subscription();
        subscription.setOrganization(org);
        subscription.setPlan(plan);
        subscription.setStatus(status);
        setId(subscription);
        return subscription;
    }

    public static Membership membership(Organization org, User user, Role role) {
        Membership membership = new Membership();
        membership.setOrganization(org);
        membership.setUser(user);
        membership.setRole(role);
        membership.setStatus(MembershipStatus.ACTIVE);
        setId(membership);
        return membership;
    }

    public static void setId(Object entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
    }
}
