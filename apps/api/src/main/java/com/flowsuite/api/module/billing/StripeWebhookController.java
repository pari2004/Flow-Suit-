package com.flowsuite.api.module.billing;

import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import com.flowsuite.api.config.StripeProperties;
import com.flowsuite.api.domain.enums.PlanCode;
import com.flowsuite.api.domain.enums.SubscriptionStatus;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

/**
 * Stripe Test Mode webhook (FR-42). Signature-verified (not JWT-authenticated) and excluded
 * from tenant usage metering. Updates the organization's subscription/limits on relevant
 * events.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Stripe webhook receiver")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeProperties stripeProperties;
    private final BillingService billingService;

    public StripeWebhookController(StripeProperties stripeProperties, BillingService billingService) {
        this.stripeProperties = stripeProperties;
        this.billingService = billingService;
    }

    @PostMapping("/stripe")
    @Operation(summary = "Receive Stripe events")
    public ResponseEntity<String> handle(@RequestBody String payload,
                                         @RequestHeader("Stripe-Signature") String signature) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, stripeProperties.webhookSecret());
        } catch (SignatureVerificationException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Invalid Stripe webhook signature.");
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            default -> log.debug("Ignoring unhandled Stripe event type: {}", event.getType());
        }
        return ResponseEntity.ok("received");
    }

    private void handleCheckoutCompleted(Event event) {
        StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
        if (!(object instanceof Session session) || session.getMetadata() == null) {
            log.warn("checkout.session.completed without a deserializable session/metadata");
            return;
        }
        Map<String, String> metadata = session.getMetadata();
        String orgIdRaw = metadata.get("organizationId");
        String planRaw = metadata.get("planCode");
        if (orgIdRaw == null || planRaw == null) {
            log.warn("checkout.session.completed missing organizationId/planCode metadata");
            return;
        }
        Instant now = Instant.now();
        billingService.applyPlanChange(
                UUID.fromString(orgIdRaw),
                PlanCode.valueOf(planRaw),
                SubscriptionStatus.ACTIVE,
                session.getCustomer(),
                session.getSubscription(),
                now,
                now.plus(30, ChronoUnit.DAYS));
        log.info("Applied plan {} to org {} from checkout session {}", planRaw, orgIdRaw, session.getId());
    }

    private void handleSubscriptionDeleted(Event event) {
        StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
        if (object instanceof Subscription subscription) {
            billingService.handleStripeSubscriptionCancelled(subscription.getId());
        }
    }
}
