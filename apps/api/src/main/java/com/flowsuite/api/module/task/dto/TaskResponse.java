package com.flowsuite.api.module.task.dto;

import com.flowsuite.api.domain.entity.Task;
import com.flowsuite.api.domain.enums.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID projectId,
        String title,
        String description,
        TaskStatus status,
        UUID assigneeId,
        UUID createdById,
        Instant dueDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse from(Task t) {
        return new TaskResponse(
                t.getId(), t.getProject().getId(), t.getTitle(), t.getDescription(), t.getStatus(),
                t.getAssignee() == null ? null : t.getAssignee().getId(),
                t.getCreatedBy() == null ? null : t.getCreatedBy().getId(),
                t.getDueDate(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
