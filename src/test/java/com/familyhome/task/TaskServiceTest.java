package com.familyhome.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.familyhome.exception.BusinessRuleException;
import com.familyhome.exception.ResourceNotFoundException;
import com.familyhome.member.AppUser;
import com.familyhome.member.AppUserRepository;
import com.familyhome.member.Role;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TaskServiceTest {

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void completesTaskOwnedByCurrentUser() {
        TaskRepository taskRepository = mock(TaskRepository.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);

        TaskService service = new TaskService(taskRepository, userRepository);

        AppUser user = mock(AppUser.class);

        when(user.getId()).thenReturn(1L);
        when(userRepository.findByUsername("dvir")).thenReturn(Optional.of(user));

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken("dvir", null, List.of()));

        OneTimeTask task = new OneTimeTask("Shopping", "Buy milk", null, LocalDate.of(2026, 11, 8));
        task.assignOwner(user);

        when(taskRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(task));

        when(taskRepository.save(task)).thenReturn(task);

        assertFalse(task.isCompleted());

        TaskResponse result = service.completeTask(10L);

        assertTrue(result.completed());
        verify(taskRepository).save(task);
    }

    @Test
    void cannotCompleteTaskOutsideCurrentUserScope() {
        TaskRepository taskRepository = mock(TaskRepository.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);

        TaskService service = new TaskService(taskRepository, userRepository);

        AppUser user = mock(AppUser.class);

        when(user.getId()).thenReturn(1L);
        when(userRepository.findByUsername("dvir")).thenReturn(Optional.of(user));

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken("dvir", null, List.of()));

        when(taskRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.completeTask(99L));
        verify(taskRepository, never()).save(any(BaseTask.class));
    }

    @Test
    void adminCanCreateTaskForAnotherMember() {
        TaskRepository taskRepository = mock(TaskRepository.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);

        TaskService service = new TaskService(taskRepository, userRepository);

        AppUser admin = mock(AppUser.class);
        when(admin.getRole()).thenReturn(Role.ADMIN);

        AppUser member = mock(AppUser.class);
        when(member.getUsername()).thenReturn("member");

        when(userRepository.findByUsername("dvir")).thenReturn(Optional.of(admin));

        when(userRepository.findById(2L)).thenReturn(Optional.of(member));

        when(taskRepository.save(any(BaseTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken("dvir", null, List.of()));

        TaskRequest request =
                new TaskRequest(
                        "Shopping",
                        "Buy milk",
                        2L,
                        TaskType.ONETIME,
                        LocalDate.of(2026, 11, 8),
                        null);

        TaskResponse result = service.createTask(request);

        assertEquals("member", result.assigneeUsername());
        assertEquals("Shopping", result.name());
        assertFalse(result.completed());

        verify(taskRepository).save(any(OneTimeTask.class));
    }

    @Test
    void adminCanUpdateOneTimeTaskDetailsDateAndAssignee() {
        TaskRepository taskRepository = mock(TaskRepository.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        TaskService service = new TaskService(taskRepository, userRepository);

        AppUser admin = mock(AppUser.class);
        when(admin.getRole()).thenReturn(Role.ADMIN);
        AppUser originalOwner = new AppUser("original", "unused");
        AppUser newOwner = new AppUser("new-owner", "unused");

        when(userRepository.findByUsername("dvir")).thenReturn(Optional.of(admin));
        when(userRepository.findById(3L)).thenReturn(Optional.of(newOwner));
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken("dvir", null, List.of()));

        OneTimeTask existing =
                new OneTimeTask("Old Name", "Old Desc", originalOwner, LocalDate.of(2026, 11, 8));
        when(taskRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(BaseTask.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate newDate = LocalDate.of(2026, 11, 20);
        TaskRequest request =
                new TaskRequest("New Name", "New Desc", 3L, TaskType.ONETIME, newDate, null);

        TaskResponse updated = service.updateTask(10L, request);

        assertEquals(TaskType.ONETIME, updated.type());
        assertEquals("New Name", updated.name());
        assertEquals("New Desc", updated.description());
        assertEquals(newDate, updated.taskDate());
        assertEquals("new-owner", updated.assigneeUsername());
        verify(taskRepository).save(existing);
    }

    @Test
    void adminCanUpdateRecurringTaskDetailsDayAndAssignee() {
        TaskRepository taskRepository = mock(TaskRepository.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        TaskService service = new TaskService(taskRepository, userRepository);

        AppUser admin = mock(AppUser.class);
        when(admin.getRole()).thenReturn(Role.ADMIN);
        AppUser originalOwner = new AppUser("original", "unused");
        AppUser newOwner = new AppUser("new-owner", "unused");

        when(userRepository.findByUsername("dvir")).thenReturn(Optional.of(admin));
        when(userRepository.findById(3L)).thenReturn(Optional.of(newOwner));
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken("dvir", null, List.of()));

        RecurringTask existing =
                new RecurringTask("Old Cleaning", "Kitchen", originalOwner, DayOfWeek.SUNDAY);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(BaseTask.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskRequest request =
                new TaskRequest(
                        "New Cleaning", "Living Room", 3L, TaskType.WEEKLY, null, DayOfWeek.FRIDAY);

        TaskResponse updated = service.updateTask(11L, request);

        assertEquals(TaskType.WEEKLY, updated.type());
        assertEquals("New Cleaning", updated.name());
        assertEquals("Living Room", updated.description());
        assertEquals(DayOfWeek.FRIDAY, updated.day());
        assertEquals("new-owner", updated.assigneeUsername());
        verify(taskRepository).save(existing);
    }

    @Test
    void updateTaskRejectsMismatchedTaskTypeWithBadRequest() {
        TaskRepository taskRepository = mock(TaskRepository.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        TaskService service = new TaskService(taskRepository, userRepository);

        AppUser admin = mock(AppUser.class);
        when(admin.getRole()).thenReturn(Role.ADMIN);
        AppUser owner = new AppUser("owner", "unused");

        when(userRepository.findByUsername("dvir")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken("dvir", null, List.of()));

        OneTimeTask existing =
                new OneTimeTask("Shopping", "Buy milk", owner, LocalDate.of(2026, 11, 8));
        when(taskRepository.findById(12L)).thenReturn(Optional.of(existing));

        TaskRequest weeklyRequest =
                new TaskRequest(
                        "Shopping", "Buy milk", 2L, TaskType.WEEKLY, null, DayOfWeek.SUNDAY);

        assertThrows(
                BusinessRuleException.class,
                () -> service.updateTask(12L, weeklyRequest));
        verify(taskRepository, never()).save(any(BaseTask.class));
    }
}
