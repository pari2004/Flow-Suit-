package com.flowsuite.api.common.audit;

import com.flowsuite.api.domain.entity.AuditLog;
import com.flowsuite.api.domain.entity.Organization;
import com.flowsuite.api.domain.entity.User;
import com.flowsuite.api.repository.AuditLogRepository;
import com.flowsuite.api.repository.OrganizationRepository;
import com.flowsuite.api.repository.UserRepository;
import com.flowsuite.api.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Writes {@link AuditLog} entries for sensitive actions (FR-14, FR-51). Always scoped to
 * the current tenant; the actor and organization are taken from the authenticated principal.
 * Uses {@code getReferenceById} to attach FKs without extra SELECTs.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository,
                        OrganizationRepository organizationRepository,
                        UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(String action, String targetType, Object targetId, Map<String, Object> metadata) {
        UUID organizationId = SecurityUtils.currentOrganizationId();
        UUID actorId = SecurityUtils.currentUserId();
        write(organizationId, actorId, action, targetType, targetId, metadata);
    }

    /** Explicit-tenant variant for flows without an authenticated principal (e.g. webhooks). */
    @Transactional
    public void recordFor(UUID organizationId, UUID actorId, String action,
                          String targetType, Object targetId, Map<String, Object> metadata) {
        write(organizationId, actorId, action, targetType, targetId, metadata);
    }

    private void write(UUID organizationId, UUID actorId, String action,
                       String targetType, Object targetId, Map<String, Object> metadata) {
        AuditLog entry = new AuditLog();
        Organization organization = organizationRepository.getReferenceById(organizationId);
        entry.setOrganization(organization);
        if (actorId != null) {
            User actor = userRepository.getReferenceById(actorId);
            entry.setActor(actor);
        }
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId == null ? null : targetId.toString());
        if (metadata != null) {
            entry.setMetadata(metadata);
        }
        auditLogRepository.save(entry);
    }
}
