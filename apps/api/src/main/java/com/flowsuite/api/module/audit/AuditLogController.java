package com.flowsuite.api.module.audit;

import com.flowsuite.api.common.audit.AuditService;
import com.flowsuite.api.common.dto.PageResponse;
import com.flowsuite.api.module.audit.dto.AuditLogResponse;
import com.flowsuite.api.repository.AuditLogRepository;
import com.flowsuite.api.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Paginated, filterable audit log for sensitive actions (FR-51). Owner/Admin only. The read
 * side lives here; writes go through {@link AuditService}.
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
@Tag(name = "Audit Logs", description = "Sensitive action history")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "List audit log entries (optionally filtered by action)")
    public PageResponse<AuditLogResponse> list(
            @RequestParam(required = false) String action,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID organizationId = SecurityUtils.currentOrganizationId();
        String actionFilter = (action == null || action.isBlank()) ? null : action;
        return PageResponse.from(
                auditLogRepository.search(organizationId, actionFilter, pageable),
                AuditLogResponse::from);
    }
}
