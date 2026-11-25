package com.flowsuite.api.module.billing;

import com.flowsuite.api.common.audit.AuditAction;
import com.flowsuite.api.common.audit.AuditService;
import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import com.flowsuite.api.config.StripeProperties;
import com.flowsuite.api.domain.entity.Plan;
import com.flowsuite.api.domain.entity.Subscription;
import com.flowsuite.api.domain.enums.PlanCode;
import com.flowsuite.api.domain.enums.SubscriptionStatus;
import com.flowsuite.api.module.billing.dto.CheckoutRequest;
import com.flowsuite.api.module.billing.dto.CheckoutResponse;
import com.flowsuite.api.module.billing.dto.InvoiceResponse;
import com.flowsuite.api.module.billing.dto.PlanResponse;
import com.flowsuite.api.module.billing.dto.SubscriptionResponse;
import com.flowsuite.api.repository.PlanRepository;
import com.flowsuite.api.repository.SubscriptionRepository;
import com.flowsuite.api.security.SecurityUtils;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceCollection;
import com.stripe.model.checkout.Session;
import com.stripe.param.InvoiceListParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Subscription + billing (FR-40..FR-45): plan catalog, Stripe checkout, and plan changes. */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AuditService auditService;
    private final StripeProperties stripeProperties;

    public BillingService(PlanRepository planRepository,
                          SubscriptionRepository subscriptionRepository,
                          AuditService auditService,
                          StripeProperties stripeProperties) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.auditService = auditService;
        this.stripeProperties = stripeProperties;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> listPlans() {
        return planRepository.findAll().stream()
                .filter(Plan::isActive)
                .sorted((a, b) -> Integer.compare(a.getPriceInr(), b.getPriceInr()))
                .map(PlanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getCurrentSubscription() {
        return SubscriptionResponse.from(requireSubscription(SecurityUtils.currentOrganizationId()));
    }

    /** FR-42: start a Stripe Test Mode checkout for an upgrade/downgrade. */
    @Transactional
    public CheckoutResponse createCheckout(CheckoutRequest request) {
        UUID organizationId = SecurityUtils.currentOrganizationId();
        if (!stripeProperties.isConfigured()) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR,
                    "Billing is not configured on the server (missing Stripe secret key).");
        }
        Plan plan = planRepository.findByCode(request.planCode())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Plan not found."));
        if (plan.getCode() == PlanCode.FREE) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "The Free plan does not require checkout.");
        }
        if (plan.getStripePriceId() == null || plan.getStripePriceId().isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "This plan has no Stripe price configured. Set plans.stripe_price_id first.");
        }

        Subscription subscription = requireSubscription(organizationId);
        Stripe.apiKey = stripeProperties.secretKey();
        try {
            SessionCreateParams.Builder params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(stripeProperties.successUrl())
                    .setCancelUrl(stripeProperties.cancelUrl())
                    .setClientReferenceId(organizationId.toString())
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(plan.getStripePriceId())
                            .setQuantity(1L)
                            .build())
                    .putMetadata("organizationId", organizationId.toString())
                    .putMetadata("planCode", plan.getCode().name());
            if (subscription.getStripeCustomerId() != null) {
                params.setCustomer(subscription.getStripeCustomerId());
            }
            Session session = Session.create(params.build());
            auditService.record(AuditAction.CHECKOUT_STARTED, "Subscription", subscription.getId(),
                    Map.of("planCode", plan.getCode().name()));
            return new CheckoutResponse(session.getUrl());
        } catch (StripeException e) {
            log.error("Stripe checkout session creation failed", e);
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Could not start checkout: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoices() {
        UUID organizationId = SecurityUtils.currentOrganizationId();
        Subscription subscription = requireSubscription(organizationId);
        if (!stripeProperties.isConfigured() || subscription.getStripeCustomerId() == null) {
            return List.of();
        }
        Stripe.apiKey = stripeProperties.secretKey();
        try {
            InvoiceCollection invoices = Invoice.list(InvoiceListParams.builder()
                    .setCustomer(subscription.getStripeCustomerId())
                    .setLimit(50L)
                    .build());
            return invoices.getData().stream()
                    .map(i -> new InvoiceResponse(i.getId(), i.getNumber(), i.getAmountPaid(),
                            i.getCurrency(), i.getStatus(), i.getHostedInvoiceUrl(), i.getCreated()))
                    .toList();
        } catch (StripeException e) {
            log.error("Fetching Stripe invoices failed", e);
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Could not fetch invoices: " + e.getMessage());
        }
    }

    /**
     * Applies a plan change to an organization's subscription (called by the Stripe webhook
     * on checkout/subscription events). Updates the limits immediately (FR-42).
     */
    @Transactional
    public void applyPlanChange(UUID organizationId, PlanCode planCode, SubscriptionStatus status,
                                String stripeCustomerId, String stripeSubscriptionId,
                                Instant periodStart, Instant periodEnd) {
        Subscription subscription = requireSubscription(organizationId);
        Plan plan = planRepository.findByCode(planCode)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Plan not found: " + planCode));

        subscription.setPlan(plan);
        subscription.setStatus(status);
        if (stripeCustomerId != null) {
            subscription.setStripeCustomerId(stripeCustomerId);
        }
        if (stripeSubscriptionId != null) {
            subscription.setStripeSubscriptionId(stripeSubscriptionId);
        }
        if (periodStart != null) {
            subscription.setCurrentPeriodStart(periodStart);
        }
        if (periodEnd != null) {
            subscription.setCurrentPeriodEnd(periodEnd);
        }

        auditService.recordFor(organizationId, null, AuditAction.SUBSCRIPTION_UPDATED, "Subscription",
                subscription.getId(), Map.of("planCode", planCode.name(), "status", status.name()));
    }

    /**
     * TEST-MODE helper: directly moves the org onto a plan as if a successful checkout had
     * occurred. Exists so entitlement/limit changes are demonstrable (and testable) without
     * live Stripe price ids. In production this path would be disabled and only the signed
     * webhook would mutate the subscription.
     */
    @Transactional
    public SubscriptionResponse simulateUpgrade(PlanCode planCode) {
        UUID organizationId = SecurityUtils.currentOrganizationId();
        Instant now = Instant.now();
        applyPlanChange(organizationId, planCode, SubscriptionStatus.ACTIVE, null, null,
                now, now.plus(30, ChronoUnit.DAYS));
        return SubscriptionResponse.from(requireSubscription(organizationId));
    }

    /** Webhook: a Stripe subscription was cancelled/deleted — mark ours cancelled. */
    @Transactional
    public void handleStripeSubscriptionCancelled(String stripeSubscriptionId) {
        subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId).ifPresent(subscription -> {
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            auditService.recordFor(subscription.getOrganization().getId(), null,
                    AuditAction.SUBSCRIPTION_CANCELLED, "Subscription", subscription.getId(),
                    Map.of("stripeSubscriptionId", stripeSubscriptionId));
        });
    }

    private Subscription requireSubscription(UUID organizationId) {
        return subscriptionRepository.findByOrganization_Id(organizationId)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No subscription found for organization."));
    }
}
