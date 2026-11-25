package com.flowsuite.api.module.task.dto;

import com.flowsuite.api.domain.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

/** Status-only update — the single mutation a Member may perform on their own task (FR-24). */
public record UpdateTaskStatusRequest(
        @NotNull TaskStatus status
) {
}
