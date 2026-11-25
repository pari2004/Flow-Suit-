package com.flowsuite.api.module.billing;

import com.flowsuite.api.module.billing.dto.PlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "Plans", description = "Subscription plan catalog")
public class PlanController {

    private final BillingService billingService;

    public PlanController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping
    @Operation(summary = "List available subscription plans")
    public List<PlanResponse> list() {
        return billingService.listPlans();
    }
}
