package com.flowsuite.api.module.billing;

import com.flowsuite.api.module.billing.dto.CheckoutRequest;
import com.flowsuite.api.module.billing.dto.CheckoutResponse;
import com.flowsuite.api.module.billing.dto.InvoiceResponse;
import com.flowsuite.api.module.billing.dto.SubscriptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Billing: subscription view (Owner/Admin) and checkout/plan changes (Owner only, FR-40). */
@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "Billing", description = "Subscription and Stripe checkout")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/subscription")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @Operation(summary = "Current subscription")
    public SubscriptionResponse subscription() {
        return billingService.getCurrentSubscription();
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @Operation(summary = "Stripe Test Mode invoice history")
    public List<InvoiceResponse> invoices() {
        return billingService.listInvoices();
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Start a Stripe checkout to change plan (Owner only)")
    public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        return billingService.createCheckout(request);
    }

    @PostMapping("/simulate-upgrade")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "TEST MODE: apply a plan change without live Stripe (Owner only)")
    public SubscriptionResponse simulateUpgrade(@Valid @RequestBody CheckoutRequest request) {
        return billingService.simulateUpgrade(request.planCode());
    }
}
