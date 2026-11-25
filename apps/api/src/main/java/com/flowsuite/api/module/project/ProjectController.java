package com.flowsuite.api.module.project;

import com.flowsuite.api.common.dto.PageResponse;
import com.flowsuite.api.module.project.dto.CreateProjectRequest;
import com.flowsuite.api.module.project.dto.ProjectResponse;
import com.flowsuite.api.module.project.dto.UpdateProjectRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Managers/Admins/Owners manage projects; Members have read access to their assigned projects. */
@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "Project management")
public class ProjectController {

    private static final String MANAGE = "hasAnyRole('OWNER','ADMIN','MANAGER')";

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @Operation(summary = "List projects (Members see only assigned projects)")
    public PageResponse<ProjectResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return projectService.list(pageable);
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get a project")
    public ProjectResponse get(@PathVariable UUID projectId) {
        return projectService.get(projectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Create a project (blocked when the plan project limit is reached)")
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(request);
    }

    @PutMapping("/{projectId}")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Update a project")
    public ProjectResponse update(@PathVariable UUID projectId,
                                  @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.update(projectId, request);
    }

    @PatchMapping("/{projectId}/archive")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Archive a project")
    public ProjectResponse archive(@PathVariable UUID projectId) {
        return projectService.setArchived(projectId, true);
    }

    @PatchMapping("/{projectId}/restore")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Restore an archived project")
    public ProjectResponse restore(@PathVariable UUID projectId) {
        return projectService.setArchived(projectId, false);
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Delete a project")
    public void delete(@PathVariable UUID projectId) {
        projectService.delete(projectId);
    }
}
