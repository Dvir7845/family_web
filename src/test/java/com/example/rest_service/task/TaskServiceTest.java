package com.example.rest_service.task;

import com.example.rest_service.member.AppUser;
import com.example.rest_service.member.AppUserRepository;
import com.example.rest_service.member.Role;

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
                verify(taskRepository, never()).delete(any(Task.class));
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

                Task task = new Task("Shopping", "Sunday", "Buy milk");
                task.assignOwner(user);

                when(taskRepository.findByIdAndOwnerId(10L, 1L))
                                .thenReturn(Optional.of(task));

                when(taskRepository.save(task)).thenReturn(task);

                assertFalse(task.isCompleted());

                Task result = service.completeTask(10L);

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
                verify(taskRepository, never()).save(any(Task.class));
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

                when(taskRepository.save(any(Task.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                                "dvir", null, List.of()));

                TaskRequest request = new TaskRequest(
                                "Shopping", "Sunday", "Buy milk", 2L);

                Task result = service.createTask(request);

                assertEquals("member", result.getAssigneeUsername());
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
                                "Shopping", "Sunday", "Buy milk", 2L);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> service.createTask(request));

                assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

                verifyNoInteractions(taskRepository);
                verify(userRepository, never()).findById(any(Long.class));
        }
}