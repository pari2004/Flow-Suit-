package com.flowsuite.api.repository;

import com.flowsuite.api.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Paginated, optionally action-filtered audit log for an org (FR-51). A null
     * {@code action} returns all entries.
     */
    @Query("""
            select a from AuditLog a
            where a.organization.id = :organizationId
              and (:action is null or a.action = :action)
            """)
    Page<AuditLog> search(@Param("organizationId") UUID organizationId,
                          @Param("action") String action,
                          Pageable pageable);
}
