package com.flowsuite.api.module.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCustomerRequest(
        @NotBlank @Size(max = 255) String name,
        @Email @Size(max = 320) String email,
        @Size(max = 50) String phone,
        @Size(max = 255) String company,
        UUID projectId
) {
}
