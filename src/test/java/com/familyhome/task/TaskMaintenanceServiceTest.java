package com.familyhome.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.familyhome.member.AppUser;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaskMaintenanceServiceTest {

    @Test
    void deletesCompletedOrExpiredOneTimeTasksAndKeepsTodayAndFutureTasks() {
        TaskRepository taskRepository = mock(TaskRepository.class);
        // 2030-03-09 22:30 UTC is already 2030-03-10 00:30 in Asia/Jerusalem (UTC+2).
        Clock clock =
                Clock.fixed(Instant.parse("2030-03-09T22:30:00Z"), ZoneId.of("Asia/Jerusalem"));
        TaskMaintenanceService maintenanceService =
                new TaskMaintenanceService(taskRepository, clock);
        AppUser owner = new AppUser("member", "unused");
        LocalDate today = LocalDate.of(2030, 3, 10);

        OneTimeTask completedOneTime = new OneTimeTask("Done once", "", owner, today.plusDays(7));
        completedOneTime.markAsCompleted("member", Instant.parse("2030-03-09T12:00:00Z"));

        OneTimeTask expiredOneTime = new OneTimeTask("Overdue once", "", owner, today.minusDays(1));
        OneTimeTask dueToday = new OneTimeTask("Due today", "", owner, today);
        OneTimeTask futureOneTime = new OneTimeTask("Due later", "", owner, today.plusDays(1));

        RecurringTask completedRecurring =
                new RecurringTask("Done weekly", "", owner, DayOfWeek.SUNDAY);
        completedRecurring.markAsCompleted("member", Instant.parse("2030-03-09T12:00:00Z"));
        RecurringTask openRecurring = new RecurringTask("Open weekly", "", owner, DayOfWeek.MONDAY);

        when(taskRepository.findAll())
                .thenReturn(
                        List.of(
                                completedOneTime,
                                expiredOneTime,
                                dueToday,
                                futureOneTime,
                                completedRecurring,
                                openRecurring));

        maintenanceService.deleteAllCompletedOneTimeTasks();

        verify(taskRepository).delete(completedOneTime);
        verify(taskRepository).delete(expiredOneTime);
        verify(taskRepository, never()).delete(dueToday);
        verify(taskRepository, never()).delete(futureOneTime);
        verify(taskRepository, never()).delete(completedRecurring);
        verify(taskRepository, never()).delete(openRecurring);
    }

    @Test
    void resetsOnlyRecurringTaskCompletionStatus() {
        TaskRepository taskRepository = mock(TaskRepository.class);
        TaskMaintenanceService maintenanceService =
                new TaskMaintenanceService(taskRepository, Clock.systemUTC());
        AppUser owner = new AppUser("member", "unused");

        RecurringTask completedRecurring = new RecurringTask("Weekly", "", owner, DayOfWeek.SUNDAY);
        completedRecurring.markAsCompleted("member", Instant.parse("2026-11-15T12:00:00Z"));

        OneTimeTask completedOneTime =
                new OneTimeTask("Once", "", owner, LocalDate.of(2026, 11, 15));
        completedOneTime.markAsCompleted("member", Instant.parse("2026-11-15T12:00:00Z"));

        when(taskRepository.findAll()).thenReturn(List.of(completedRecurring, completedOneTime));

        maintenanceService.resetWeeklyTaskCompletionStatus();

        assertFalse(completedRecurring.isCompleted());
        assertNull(completedRecurring.getCompletedAt());
        assertNull(completedRecurring.getCompletedByUsername());

        assertTrue(completedOneTime.isCompleted());
        assertNotNull(completedOneTime.getCompletedAt());
        assertEquals("member", completedOneTime.getCompletedByUsername());
    }
}
