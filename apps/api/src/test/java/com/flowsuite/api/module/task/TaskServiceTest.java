package com.flowsuite.api.module.task;

import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import com.flowsuite.api.domain.entity.Organization;
import com.flowsuite.api.domain.entity.Project;
import com.flowsuite.api.domain.entity.Task;
import com.flowsuite.api.domain.entity.User;
import com.flowsuite.api.domain.enums.Role;
import com.flowsuite.api.domain.enums.TaskStatus;
import com.flowsuite.api.module.task.dto.TaskResponse;
import com.flowsuite.api.module.task.dto.UpdateTaskStatusRequest;
import com.flowsuite.api.repository.MembershipRepository;
import com.flowsuite.api.repository.OrganizationRepository;
import com.flowsuite.api.repository.ProjectRepository;
import com.flowsuite.api.repository.TaskRepository;
import com.flowsuite.api.repository.UserRepository;
import com.flowsuite.api.support.SecurityContextSupport;
import com.flowsuite.api.support.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock ProjectRepository projectRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock UserRepository userRepository;

    @InjectMocks TaskService taskService;

    @AfterEach
    void tearDown() {
        SecurityContextSupport.clear();
    }

    private Task taskAssignedTo(Organization org, User assignee) {
        Project project = new Project();
        project.setOrganization(org);
        TestFixtures.setId(project);

        Task task = new Task();
        task.setOrganization(org);
        task.setProject(project);
        task.setTitle("Task");
        task.setStatus(TaskStatus.TODO);
        task.setAssignee(assignee);
        TestFixtures.setId(task);
        return task;
    }

    @Test
    void member_canUpdateStatusOfOwnTask() {
        Organization org = TestFixtures.organization("Acme");
        User member = TestFixtures.user("m@acme.com", "Member", "h");
        Task task = taskAssignedTo(org, member);

        SecurityContextSupport.authenticate(member.getId(), org.getId(), Role.MEMBER);
        when(taskRepository.findByIdAndOrganization_Id(task.getId(), org.getId())).thenReturn(Optional.of(task));

        TaskResponse response = taskService.updateStatus(task.getId(),
                new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS));

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void member_cannotUpdateStatusOfSomeoneElsesTask() {
        Organization org = TestFixtures.organization("Acme");
        User otherAssignee = TestFixtures.user("o@acme.com", "Other", "h");
        Task task = taskAssignedTo(org, otherAssignee);

        UUID callerId = UUID.randomUUID();
        SecurityContextSupport.authenticate(callerId, org.getId(), Role.MEMBER);
        when(taskRepository.findByIdAndOrganization_Id(task.getId(), org.getId())).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(task.getId(),
                new UpdateTaskStatusRequest(TaskStatus.DONE)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void manager_canUpdateAnyTaskStatus() {
        Organization org = TestFixtures.organization("Acme");
        User someAssignee = TestFixtures.user("o@acme.com", "Other", "h");
        Task task = taskAssignedTo(org, someAssignee);

        SecurityContextSupport.authenticate(UUID.randomUUID(), org.getId(), Role.MANAGER);
        when(taskRepository.findByIdAndOrganization_Id(task.getId(), org.getId())).thenReturn(Optional.of(task));

        TaskResponse response = taskService.updateStatus(task.getId(),
                new UpdateTaskStatusRequest(TaskStatus.DONE));

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
    }
}
