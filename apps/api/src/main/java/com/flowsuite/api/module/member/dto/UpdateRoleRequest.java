package com.flowsuite.api.module.member.dto;

import com.flowsuite.api.domain.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull Role role
) {
}
