package com.flowsuite.api.repository;

import com.flowsuite.api.domain.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findByOrganization_Id(UUID organizationId, Pageable pageable);

    Page<Task> findByOrganization_IdAndProject_Id(UUID organizationId, UUID projectId, Pageable pageable);

    /** Tasks assigned to a specific user (Member self-scope, FR-24). */
    Page<Task> findByOrganization_IdAndAssignee_Id(UUID organizationId, UUID assigneeId, Pageable pageable);

    Page<Task> findByOrganization_IdAndProject_IdAndAssignee_Id(
            UUID organizationId, UUID projectId, UUID assigneeId, Pageable pageable);

    Optional<Task> findByIdAndOrganization_Id(UUID id, UUID organizationId);

    /** Whether a user has any task assigned to them in a project (Member visibility check). */
    boolean existsByOrganization_IdAndProject_IdAndAssignee_Id(
            UUID organizationId, UUID projectId, UUID assigneeId);
}
