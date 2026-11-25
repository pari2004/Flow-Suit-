package com.flowsuite.api.module.project;

import com.flowsuite.api.common.dto.PageResponse;
import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import com.flowsuite.api.domain.entity.Project;
import com.flowsuite.api.domain.enums.ProjectStatus;
import com.flowsuite.api.domain.enums.Role;
import com.flowsuite.api.module.billing.EntitlementService;
import com.flowsuite.api.module.project.dto.CreateProjectRequest;
import com.flowsuite.api.module.project.dto.ProjectResponse;
import com.flowsuite.api.module.project.dto.UpdateProjectRequest;
import com.flowsuite.api.repository.OrganizationRepository;
import com.flowsuite.api.repository.ProjectRepository;
import com.flowsuite.api.repository.TaskRepository;
import com.flowsuite.api.repository.UserRepository;
import com.flowsuite.api.security.AuthPrincipal;
import com.flowsuite.api.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Projects CRUD + archive, tenant-scoped with Member self-scope (FR-20..FR-24). */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final EntitlementService entitlementService;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository,
                          TaskRepository taskRepository,
                          EntitlementService entitlementService,
                          OrganizationRepository organizationRepository,
                          UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.entitlementService = entitlementService;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(Pageable pageable) {
        AuthPrincipal principal = SecurityUtils.currentPrincipal();
        if (principal.role() == Role.MEMBER) {
            // Members see only projects they are assigned tasks in (FR-24).
            return PageResponse.from(
                    projectRepository.findAssignedToUser(principal.organizationId(), principal.userId(), pageable),
                    ProjectResponse::from);
        }
        return PageResponse.from(
                projectRepository.findByOrganization_Id(principal.organizationId(), pageable),
                ProjectResponse::from);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID projectId) {
        return ProjectResponse.from(loadVisible(projectId));
    }

    /** FR-20: create a project; blocked when the plan's project limit is reached (FR-44). */
    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        UUID organizationId = SecurityUtils.currentOrganizationId();
        entitlementService.assertCanAddProject(organizationId);

        Project project = new Project();
        project.setOrganization(organizationRepository.getReferenceById(organizationId));
        project.setName(request.name().trim());
        project.setDescription(request.description());
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedBy(userRepository.getReferenceById(SecurityUtils.currentUserId()));
        projectRepository.save(project);
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse update(UUID projectId, UpdateProjectRequest request) {
        Project project = loadOwned(projectId);
        project.setName(request.name().trim());
        project.setDescription(request.description());
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse setArchived(UUID projectId, boolean archived) {
        Project project = loadOwned(projectId);
        project.setStatus(archived ? ProjectStatus.ARCHIVED : ProjectStatus.ACTIVE);
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(UUID projectId) {
        Project project = loadOwned(projectId);
        projectRepository.delete(project);
    }

    // --- internal tenant-scoped loaders ---

    /** Load with Member visibility rules; returns 404 for anything outside the caller's scope. */
    private Project loadVisible(UUID projectId) {
        AuthPrincipal principal = SecurityUtils.currentPrincipal();
        Project project = projectRepository.findByIdAndOrganization_Id(projectId, principal.organizationId())
                .orElseThrow(ProjectService::notFound);
        if (principal.role() == Role.MEMBER
                && !taskRepository.existsByOrganization_IdAndProject_IdAndAssignee_Id(
                        principal.organizationId(), projectId, principal.userId())) {
            throw notFound();
        }
        return project;
    }

    /** Load for mutation (manager+); tenant-scoped, 404 if not in the caller's org. */
    private Project loadOwned(UUID projectId) {
        UUID organizationId = SecurityUtils.currentOrganizationId();
        return projectRepository.findByIdAndOrganization_Id(projectId, organizationId)
                .orElseThrow(ProjectService::notFound);
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.NOT_FOUND, "Project not found.");
    }
}
