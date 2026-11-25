package com.flowsuite.api.module.project.dto;

import com.flowsuite.api.domain.entity.Project;
import com.flowsuite.api.domain.enums.ProjectStatus;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String description,
        ProjectStatus status,
        UUID createdById,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponse from(Project p) {
        return new ProjectResponse(
                p.getId(), p.getName(), p.getDescription(), p.getStatus(),
                p.getCreatedBy() == null ? null : p.getCreatedBy().getId(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
