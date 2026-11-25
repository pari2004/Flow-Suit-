package com.flowsuite.api.module.billing.dto;

/** The Stripe-hosted checkout URL the client should redirect to. */
public record CheckoutResponse(String checkoutUrl) {
}
