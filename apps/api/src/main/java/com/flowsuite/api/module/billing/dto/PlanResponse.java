package com.flowsuite.api.module.billing.dto;

import com.flowsuite.api.domain.entity.Plan;
import com.flowsuite.api.domain.enums.PlanCode;

import java.util.Map;

public record PlanResponse(
        PlanCode code,
        String name,
        int priceInr,
        int seatLimit,
        Integer projectLimit,
        int apiRequestLimit,
        Map<String, Object> entitlements
) {
    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
                plan.getCode(), plan.getName(), plan.getPriceInr(), plan.getSeatLimit(),
                plan.getProjectLimit(), plan.getApiRequestLimit(), plan.getEntitlements());
    }
}
