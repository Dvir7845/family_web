package com.familyhome.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.familyhome.exception.BusinessRuleException;
import com.familyhome.member.AppUser;
import com.familyhome.member.AppUserRepository;
import com.familyhome.member.Role;
import java.time.Clock;
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
    private final TaskService service = new TaskService(tasks, users, Clock.systemUTC());
    private final AppUser member = new AppUser("member", "unused");

    @BeforeEach
    void authenticateAdmin() {
        AppUser admin = mock(AppUser.class);
        when(admin.getRole()).thenReturn(Role.ADMIN);
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(users.findById(2L)).thenReturn(Optional.of(member));
        when(tasks.save(any(BaseTask.class))).thenAnswer(call -> call.getArgument(0));
        when(tasks.saveAll(anyIterable())).thenAnswer(call -> call.getArgument(0));
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
        List<TaskResponse> onceList =
                service.createTask(
                        new TaskRequest("Shopping", "Buy milk", 2L, TaskType.ONETIME, date, null));
        List<TaskResponse> weeklyList =
                service.createTask(
                        new TaskRequest(
                                "Cleaning",
                                "Kitchen",
                                2L,
                                TaskType.WEEKLY,
                                null,
                                List.of(DayOfWeek.SUNDAY, DayOfWeek.TUESDAY, DayOfWeek.SUNDAY)));

        assertEquals(1, onceList.size());
        TaskResponse once = onceList.getFirst();
        assertEquals(TaskType.ONETIME, once.type());
        assertEquals(date, once.taskDate());
        assertEquals("member", once.assigneeUsername());
        assertFalse(once.completed());

        assertEquals(2, weeklyList.size());
        assertEquals(TaskType.WEEKLY, weeklyList.get(0).type());
        assertEquals(DayOfWeek.SUNDAY, weeklyList.get(0).day());
        assertEquals(DayOfWeek.TUESDAY, weeklyList.get(1).day());
        assertEquals("member", weeklyList.get(0).assigneeUsername());
        assertFalse(weeklyList.get(0).completed());
    }

    @Test
    void rejectsMissingAndConflictingScheduleFieldsBeforeSaving() {
        for (TaskRequest request :
                List.of(
                        new TaskRequest("Task", "", 2L, TaskType.ONETIME, null, null),
                        new TaskRequest("Task", "", 2L, TaskType.WEEKLY, null, null),
                        new TaskRequest("Task", "", 2L, TaskType.WEEKLY, null, List.of()),
                        new TaskRequest(
                                "Task",
                                "",
                                2L,
                                TaskType.ONETIME,
                                LocalDate.of(2026, 11, 15),
                                List.of(DayOfWeek.SUNDAY)),
                        new TaskRequest(
                                "Task",
                                "",
                                2L,
                                TaskType.WEEKLY,
                                LocalDate.of(2026, 11, 15),
                                List.of(DayOfWeek.SUNDAY)))) {
            assertThrows(BusinessRuleException.class, () -> service.createTask(request));
        }
        verifyNoInteractions(tasks);
        verify(users, never()).findById(anyLong());
    }
}
