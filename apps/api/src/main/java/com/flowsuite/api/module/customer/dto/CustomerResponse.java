package com.flowsuite.api.module.customer.dto;

import com.flowsuite.api.domain.entity.Customer;
import com.flowsuite.api.domain.enums.CustomerStatus;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String company,
        UUID projectId,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getCompany(),
                c.getProject() == null ? null : c.getProject().getId(),
                c.getStatus(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
