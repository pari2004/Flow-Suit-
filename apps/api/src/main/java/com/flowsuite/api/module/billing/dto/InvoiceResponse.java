package com.flowsuite.api.module.billing.dto;

public record InvoiceResponse(
        String id,
        String number,
        Long amountPaid,
        String currency,
        String status,
        String hostedInvoiceUrl,
        Long created
) {
}
