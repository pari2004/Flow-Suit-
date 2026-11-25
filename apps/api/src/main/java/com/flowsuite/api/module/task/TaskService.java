package com.flowsuite.api.module.task;

import com.flowsuite.api.common.dto.PageResponse;
import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import com.flowsuite.api.domain.entity.Membership;
import com.flowsuite.api.domain.entity.Project;
import com.flowsuite.api.domain.entity.Task;
import com.flowsuite.api.domain.enums.MembershipStatus;
import com.flowsuite.api.domain.enums.Role;
import com.flowsuite.api.domain.enums.TaskStatus;
import com.flowsuite.api.module.task.dto.CreateTaskRequest;
import com.flowsuite.api.module.task.dto.TaskResponse;
import com.flowsuite.api.module.task.dto.UpdateTaskRequest;
import com.flowsuite.api.module.task.dto.UpdateTaskStatusRequest;
import com.flowsuite.api.repository.MembershipRepository;
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

/** Tasks CRUD, assignment, and status updates (FR-22..FR-24). */
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final MembershipRepository membershipRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       MembershipRepository membershipRepository,
                       OrganizationRepository organizationRepository,
                       UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(UUID projectId, Pageable pageable) {
        AuthPrincipal principal = SecurityUtils.currentPrincipal();
        UUID orgId = principal.organizationId();
        boolean memberScope = principal.role() == Role.MEMBER;

        var page = switch (toFilter(projectId, memberScope)) {
            case ORG_ALL -> taskRepository.findByOrganization_Id(orgId, pageable);
            case ORG_PROJECT -> taskRepository.findByOrganization_IdAndProject_Id(orgId, projectId, pageable);
            case MINE_ALL -> taskRepository.findByOrganization_IdAndAssignee_Id(orgId, principal.userId(), pageable);
            case MINE_PROJECT -> taskRepository.findByOrganization_IdAndProject_IdAndAssignee_Id(
                    orgId, projectId, principal.userId(), pageable);
        };
        return PageResponse.from(page, TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(UUID taskId) {
        return TaskResponse.from(loadVisible(taskId));
    }

    /** FR-22: Managers+ create/assign tasks. */
    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        UUID orgId = SecurityUtils.currentOrganizationId();
        Project project = projectRepository.findByIdAndOrganization_Id(request.projectId(), orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Project not found."));

        Task task = new Task();
        task.setOrganization(organizationRepository.getReferenceById(orgId));
        task.setProject(project);
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(request.dueDate());
        assignTo(task, request.assigneeId(), orgId);
        task.setCreatedBy(userRepository.getReferenceById(SecurityUtils.currentUserId()));
        taskRepository.save(task);
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse update(UUID taskId, UpdateTaskRequest request) {
        UUID orgId = SecurityUtils.currentOrganizationId();
        Task task = loadOwned(taskId, orgId);
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        assignTo(task, request.assigneeId(), orgId);
        return TaskResponse.from(task);
    }

    /**
     * FR-24: update a task's status. Members may only transition tasks assigned to them;
     * Managers+ may update any task in the org.
     */
    @Transactional
    public TaskResponse updateStatus(UUID taskId, UpdateTaskStatusRequest request) {
        AuthPrincipal principal = SecurityUtils.currentPrincipal();
        Task task = loadOwned(taskId, principal.organizationId());
        if (principal.role() == Role.MEMBER) {
            boolean assignedToMe = task.getAssignee() != null
                    && task.getAssignee().getId().equals(principal.userId());
            if (!assignedToMe) {
                throw new ApiException(ErrorCode.FORBIDDEN, "You can only update tasks assigned to you.");
            }
        }
        task.setStatus(request.status());
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(UUID taskId) {
        Task task = loadOwned(taskId, SecurityUtils.currentOrganizationId());
        taskRepository.delete(task);
    }

    // --- helpers ---

    /** Sets or clears the assignee, validating that an assignee is an active member of the org. */
    private void assignTo(Task task, UUID assigneeId, UUID orgId) {
        if (assigneeId == null) {
            task.setAssignee(null);
            return;
        }
        Membership membership = membershipRepository.findByOrganization_IdAndUser_Id(orgId, assigneeId)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST,
                        "Assignee is not a member of this organization."));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Assignee is not an active member.");
        }
        task.setAssignee(userRepository.getReferenceById(assigneeId));
    }

    private Task loadVisible(UUID taskId) {
        AuthPrincipal principal = SecurityUtils.currentPrincipal();
        Task task = taskRepository.findByIdAndOrganization_Id(taskId, principal.organizationId())
                .orElseThrow(TaskService::notFound);
        if (principal.role() == Role.MEMBER) {
            boolean assignedToMe = task.getAssignee() != null
                    && task.getAssignee().getId().equals(principal.userId());
            if (!assignedToMe) {
                throw notFound();
            }
        }
        return task;
    }

    private Task loadOwned(UUID taskId, UUID orgId) {
        return taskRepository.findByIdAndOrganization_Id(taskId, orgId)
                .orElseThrow(TaskService::notFound);
    }

    private static Filter toFilter(UUID projectId, boolean memberScope) {
        if (memberScope) {
            return projectId == null ? Filter.MINE_ALL : Filter.MINE_PROJECT;
        }
        return projectId == null ? Filter.ORG_ALL : Filter.ORG_PROJECT;
    }

    private enum Filter { ORG_ALL, ORG_PROJECT, MINE_ALL, MINE_PROJECT }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.NOT_FOUND, "Task not found.");
    }
}
