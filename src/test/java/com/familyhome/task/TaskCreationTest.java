package com.familyhome.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.familyhome.exception.BusinessRuleException;
import com.familyhome.member.AppUser;
import com.familyhome.member.AppUserRepository;
import com.familyhome.member.Role;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TaskCreationTest {
    private final TaskRepository tasks = mock(TaskRepository.class);
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final TaskService service = new TaskService(tasks, users);
    private final AppUser member = new AppUser("member", "unused");

    @BeforeEach
    void authenticateAdmin() {
        AppUser admin = mock(AppUser.class);
        when(admin.getRole()).thenReturn(Role.ADMIN);
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(users.findById(2L)).thenReturn(Optional.of(member));
        when(tasks.save(any(BaseTask.class))).thenAnswer(call -> call.getArgument(0));
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken("admin", null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsBothTaskTypesWithTheirScheduleAndOwner() {
        LocalDate date = LocalDate.of(2026, 11, 15);
        TaskResponse once =
                service.createTask(
                        new TaskRequest("Shopping", "Buy milk", 2L, TaskType.ONETIME, date, null));
        TaskResponse weekly =
                service.createTask(
                        new TaskRequest(
                                "Cleaning", "Kitchen", 2L, TaskType.WEEKLY, null, DayOfWeek.SUNDAY));
        assertEquals(TaskType.ONETIME, once.type());
        assertEquals(TaskType.WEEKLY, weekly.type());
        assertEquals(date, once.taskDate());
        assertEquals(DayOfWeek.SUNDAY, weekly.day());
        assertEquals("member", once.assigneeUsername());
        assertEquals("member", weekly.assigneeUsername());
        assertFalse(once.completed());
        assertFalse(weekly.completed());
    }

    @Test
    void rejectsMissingAndConflictingScheduleFieldsBeforeSaving() {
        for (TaskRequest request :
                List.of(
                        new TaskRequest("Task", "", 2L, TaskType.ONETIME, null, null),
                        new TaskRequest("Task", "", 2L, TaskType.WEEKLY, null, null),
                        new TaskRequest(
                                "Task",
                                "",
                                2L,
                                TaskType.ONETIME,
                                LocalDate.of(2026, 11, 15),
                                DayOfWeek.SUNDAY),
                        new TaskRequest(
                                "Task",
                                "",
                                2L,
                                TaskType.WEEKLY,
                                LocalDate.of(2026, 11, 15),
                                DayOfWeek.SUNDAY))) {
            assertThrows(BusinessRuleException.class, () -> service.createTask(request));
        }
        verifyNoInteractions(tasks);
        verify(users, never()).findById(anyLong());
    }
}
