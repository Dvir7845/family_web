package com.familyhome.task;

import com.familyhome.member.AppUser;
import com.familyhome.member.AppUserRepository;
import com.familyhome.member.Role;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskServiceTest {

        @AfterEach
        void clearAuthentication() {
                SecurityContextHolder.clearContext();
        }

        @Test
        void memberCannotDeleteTask() {

                TaskRepository taskRepository = mock(TaskRepository.class);
                AppUserRepository userRepository = mock(AppUserRepository.class);

                TaskService service = new TaskService(taskRepository, userRepository);

                AppUser user = mock(AppUser.class);

                when(user.getRole()).thenReturn(Role.MEMBER);
                when(user.getId()).thenReturn(1L);
                when(userRepository.findByUsername("dvir"))
                                .thenReturn(Optional.of(user));
                // Set the authentication context to simulate a logged-in user
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                                "dvir", null, List.of()));

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> service.deleteTask(99L));

                assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
                // Verify that the delete method was never called
                verify(taskRepository, never()).delete(any(BaseTask.class));
        }

        @Test
        void completesTaskOwnedByCurrentUser() {
                TaskRepository taskRepository = mock(TaskRepository.class);
                AppUserRepository userRepository = mock(AppUserRepository.class);

                TaskService service = new TaskService(taskRepository, userRepository);

                AppUser user = mock(AppUser.class);

                when(user.getId()).thenReturn(1L);
                when(userRepository.findByUsername("dvir"))
                                .thenReturn(Optional.of(user));

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                                "dvir", null, List.of()));

                OneTimeTask task = new OneTimeTask("Shopping", "Buy milk", null, java.time.LocalDate.of(2026, 11, 8));
                task.assignOwner(user);

                when(taskRepository.findByIdAndOwnerId(10L, 1L))
                                .thenReturn(Optional.of(task));

                when(taskRepository.save(task)).thenReturn(task);

                assertFalse(task.isCompleted());

                BaseTask result = service.completeTask(10L);

                assertTrue(result.isCompleted());
                verify(taskRepository).save(task);
        }

        @Test
        void cannotCompleteTaskOutsideCurrentUserScope() {
                TaskRepository taskRepository = mock(TaskRepository.class);
                AppUserRepository userRepository = mock(AppUserRepository.class);

                TaskService service = new TaskService(taskRepository, userRepository);

                AppUser user = mock(AppUser.class);

                when(user.getId()).thenReturn(1L);
                when(userRepository.findByUsername("dvir"))
                                .thenReturn(Optional.of(user));

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                                "dvir", null, List.of()));

                when(taskRepository.findByIdAndOwnerId(99L, 1L))
                                .thenReturn(Optional.empty());

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> service.completeTask(99L));

                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
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

                when(userRepository.findByUsername("dvir"))
                                .thenReturn(Optional.of(admin));

                when(userRepository.findById(2L))
                                .thenReturn(Optional.of(member));

                when(taskRepository.save(any(BaseTask.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                                "dvir", null, List.of()));

                TaskRequest request = new TaskRequest(
                                "Shopping", "Buy milk", 2L, TaskType.ONETIME, java.time.LocalDate.of(2026, 11, 8), null);

                BaseTask result = service.createTask(request);

                assertEquals("member", result.getOwner().getUsername());
                assertEquals("Shopping", result.getName());
                assertFalse(result.isCompleted());

                verify(taskRepository).save(result);
        }

        @Test
        void memberCannotCreateTask() {
                TaskRepository taskRepository = mock(TaskRepository.class);
                AppUserRepository userRepository = mock(AppUserRepository.class);

                TaskService service = new TaskService(taskRepository, userRepository);

                AppUser member = mock(AppUser.class);
                when(member.getRole()).thenReturn(Role.MEMBER);

                when(userRepository.findByUsername("member"))
                                .thenReturn(Optional.of(member));

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                                "member", null, List.of()));

                TaskRequest request = new TaskRequest(
                                "Shopping", "Buy milk", 2L, TaskType.ONETIME, java.time.LocalDate.of(2026, 11, 8), null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> service.createTask(request));

                assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

                verifyNoInteractions(taskRepository);
                verify(userRepository, never()).findById(any(Long.class));
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
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken("dvir", null, List.of()));

                OneTimeTask existing = new OneTimeTask(
                                "Old Name", "Old Desc", originalOwner, java.time.LocalDate.of(2026, 11, 8));
                when(taskRepository.findById(10L)).thenReturn(Optional.of(existing));
                when(taskRepository.save(any(BaseTask.class))).thenAnswer(inv -> inv.getArgument(0));

                java.time.LocalDate newDate = java.time.LocalDate.of(2026, 11, 20);
                TaskRequest request = new TaskRequest(
                                "New Name", "New Desc", 3L, TaskType.ONETIME, newDate, null);

                BaseTask updated = service.updateTask(10L, request);

                OneTimeTask updatedOneTime = assertInstanceOf(OneTimeTask.class, updated);
                assertEquals("New Name", updatedOneTime.getName());
                assertEquals("New Desc", updatedOneTime.getDescription());
                assertEquals(newDate, updatedOneTime.getTaskDate());
                assertSame(newOwner, updatedOneTime.getOwner());
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
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken("dvir", null, List.of()));

                RecurringTask existing = new RecurringTask(
                                "Old Cleaning", "Kitchen", originalOwner, java.time.DayOfWeek.SUNDAY);
                when(taskRepository.findById(11L)).thenReturn(Optional.of(existing));
                when(taskRepository.save(any(BaseTask.class))).thenAnswer(inv -> inv.getArgument(0));

                TaskRequest request = new TaskRequest(
                                "New Cleaning", "Living Room", 3L, TaskType.WEEKLY, null, java.time.DayOfWeek.FRIDAY);

                BaseTask updated = service.updateTask(11L, request);

                RecurringTask updatedRecurring = assertInstanceOf(RecurringTask.class, updated);
                assertEquals("New Cleaning", updatedRecurring.getName());
                assertEquals("Living Room", updatedRecurring.getDescription());
                assertEquals(java.time.DayOfWeek.FRIDAY, updatedRecurring.getDay());
                assertSame(newOwner, updatedRecurring.getOwner());
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
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken("dvir", null, List.of()));

                OneTimeTask existing = new OneTimeTask(
                                "Shopping", "Buy milk", owner, java.time.LocalDate.of(2026, 11, 8));
                when(taskRepository.findById(12L)).thenReturn(Optional.of(existing));

                TaskRequest weeklyRequest = new TaskRequest(
                                "Shopping", "Buy milk", 2L, TaskType.WEEKLY, null, java.time.DayOfWeek.SUNDAY);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> service.updateTask(12L, weeklyRequest));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                verify(taskRepository, never()).save(any(BaseTask.class));
        }
}