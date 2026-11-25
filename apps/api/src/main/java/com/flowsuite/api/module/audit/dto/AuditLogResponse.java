package com.flowsuite.api.module.audit.dto;

import com.flowsuite.api.domain.entity.AuditLog;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String action,
        String targetType,
        String targetId,
        Map<String, Object> metadata,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(
                a.getId(),
                a.getActor() == null ? null : a.getActor().getId(),
                a.getAction(), a.getTargetType(), a.getTargetId(),
                a.getMetadata(), a.getCreatedAt());
    }
}
