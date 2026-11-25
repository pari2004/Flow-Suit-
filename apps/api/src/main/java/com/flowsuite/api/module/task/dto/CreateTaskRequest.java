package com.flowsuite.api.module.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateTaskRequest(
        @NotNull UUID projectId,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String description,
        UUID assigneeId,
        Instant dueDate
) {
}
