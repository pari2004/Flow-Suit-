package com.flowsuite.api.repository;

import com.flowsuite.api.domain.entity.Project;
import com.flowsuite.api.domain.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Page<Project> findByOrganization_Id(UUID organizationId, Pageable pageable);

    /** Tenant-scoped single fetch — the lookup that makes cross-tenant access return 404. */
    Optional<Project> findByIdAndOrganization_Id(UUID id, UUID organizationId);

    /** Count of active projects in an org — used to enforce the plan project limit (FR-20, FR-44). */
    long countByOrganization_IdAndStatus(UUID organizationId, ProjectStatus status);

    /**
     * Projects a Member can see: those containing at least one task assigned to them
     * (FR-24 — members see only assigned projects).
     */
    @Query("""
            select distinct p from Project p
            join Task t on t.project = p
            where p.organization.id = :organizationId
              and t.assignee.id = :userId
            """)
    Page<Project> findAssignedToUser(@Param("organizationId") UUID organizationId,
                                     @Param("userId") UUID userId,
                                     Pageable pageable);
}
