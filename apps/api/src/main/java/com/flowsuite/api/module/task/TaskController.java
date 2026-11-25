package com.flowsuite.api.module.task;

import com.flowsuite.api.common.dto.PageResponse;
import com.flowsuite.api.module.task.dto.CreateTaskRequest;
import com.flowsuite.api.module.task.dto.TaskResponse;
import com.flowsuite.api.module.task.dto.UpdateTaskRequest;
import com.flowsuite.api.module.task.dto.UpdateTaskStatusRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks", description = "Task management and assignment")
public class TaskController {

    private static final String MANAGE = "hasAnyRole('OWNER','ADMIN','MANAGER')";

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "List tasks (Members see only their assigned tasks); optional projectId filter")
    public PageResponse<TaskResponse> list(@RequestParam(required = false) UUID projectId,
                                           @PageableDefault(size = 20) Pageable pageable) {
        return taskService.list(projectId, pageable);
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get a task")
    public TaskResponse get(@PathVariable UUID taskId) {
        return taskService.get(taskId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Create and optionally assign a task (Manager+)")
    public TaskResponse create(@Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(request);
    }

    @PutMapping("/{taskId}")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Update a task (Manager+)")
    public TaskResponse update(@PathVariable UUID taskId, @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(taskId, request);
    }

    @PatchMapping("/{taskId}/status")
    @Operation(summary = "Update task status (Members may update only their own assigned tasks)")
    public TaskResponse updateStatus(@PathVariable UUID taskId,
                                     @Valid @RequestBody UpdateTaskStatusRequest request) {
        return taskService.updateStatus(taskId, request);
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Delete a task (Manager+)")
    public void delete(@PathVariable UUID taskId) {
        taskService.delete(taskId);
    }
}
