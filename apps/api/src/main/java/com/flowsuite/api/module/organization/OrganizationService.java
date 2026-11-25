package com.flowsuite.api.module.organization;

import com.flowsuite.api.common.audit.AuditAction;
import com.flowsuite.api.common.audit.AuditService;
import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import com.flowsuite.api.domain.entity.Organization;
import com.flowsuite.api.module.organization.dto.OrganizationResponse;
import com.flowsuite.api.module.organization.dto.UpdateOrganizationRequest;
import com.flowsuite.api.repository.OrganizationRepository;
import com.flowsuite.api.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/** The authenticated user's own organization (FR-10..FR-14 settings portions). */
@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;

    public OrganizationService(OrganizationRepository organizationRepository, AuditService auditService) {
        this.organizationRepository = organizationRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getCurrent() {
        return OrganizationResponse.from(loadCurrent());
    }

    @Transactional
    public OrganizationResponse update(UpdateOrganizationRequest request) {
        Organization organization = loadCurrent();
        organization.setName(request.name().trim());
        auditService.record(AuditAction.ORGANIZATION_UPDATED, "Organization", organization.getId(),
                Map.of("name", organization.getName()));
        return OrganizationResponse.from(organization);
    }

    @Transactional
    public void delete() {
        Organization organization = loadCurrent();
        // Audit before the cascade removes the org (and its audit rows).
        auditService.record(AuditAction.ORGANIZATION_DELETED, "Organization", organization.getId(), Map.of());
        organizationRepository.delete(organization);
    }

    private Organization loadCurrent() {
        UUID organizationId = SecurityUtils.currentOrganizationId();
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Organization not found."));
    }
}
