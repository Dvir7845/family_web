package com.familyhome.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.familyhome.config.TimeConfig;
import com.familyhome.member.AppUser;
import com.familyhome.member.AppUserRepository;
import com.familyhome.security.AuthService;
import com.familyhome.security.SecurityConfig;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {SecurityConfig.class, AuthService.class, TaskService.class, TimeConfig.class})
class TaskServiceSecurityTest {

    @Autowired private TaskService taskService;

    @MockitoBean private TaskRepository taskRepository;

    @MockitoBean private AppUserRepository userRepository;

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void memberCannotCreateOrDeleteTasks() {
        AppUser member = new AppUser("member", "unused");
        when(userRepository.findByUsername("member")).thenReturn(Optional.of(member));

        TaskRequest request =
                new TaskRequest(
                        "Shopping",
                        "Buy milk",
                        2L,
                        TaskType.ONETIME,
                        LocalDate.of(2026, 11, 8),
                        null);

        assertThrows(AccessDeniedException.class, () -> taskService.createTask(request));
        assertThrows(AccessDeniedException.class, () -> taskService.deleteTask(99L));

        verifyNoInteractions(taskRepository);
    }

    @Test
    @WithMockUser(username = "task_manager", roles = "MEMBER")
    void taskManagerCanCreateTasksWithoutAdminRole() {
        AppUser manager = new AppUser("task_manager", "unused");
        manager.changeTaskManager(true);
        AppUser assignee = new AppUser("member", "unused");

        when(userRepository.findByUsername("task_manager")).thenReturn(Optional.of(manager));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(taskRepository.save(any(BaseTask.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskRequest request =
                new TaskRequest(
                        "Shopping",
                        "Buy milk",
                        2L,
                        TaskType.ONETIME,
                        LocalDate.of(2026, 11, 8),
                        null);

        List<TaskResponse> responses = taskService.createTask(request);

        assertEquals(1, responses.size());
        TaskResponse response = responses.getFirst();
        assertEquals("Shopping", response.name());
        assertEquals("member", response.assigneeUsername());
        verify(taskRepository).save(any(OneTimeTask.class));
    }
}
