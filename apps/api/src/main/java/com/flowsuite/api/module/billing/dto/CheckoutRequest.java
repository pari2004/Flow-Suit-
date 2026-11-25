package com.flowsuite.api.module.billing.dto;

import com.flowsuite.api.domain.enums.PlanCode;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull PlanCode planCode
) {
}
