package com.familyhome.task;

import com.familyhome.member.AppUser;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskMaintenanceServiceTest {

    @Test
    void deletesCompletedOrExpiredOneTimeTasksAndKeepsTodayAndFutureTasks() {
        TaskRepository taskRepository = mock(TaskRepository.class);
        TaskMaintenanceService maintenanceService = new TaskMaintenanceService(taskRepository);
        AppUser owner = new AppUser("member", "unused");
        LocalDate today = LocalDate.of(2030, 3, 10);

        OneTimeTask completedOneTime = new OneTimeTask("Done once", "", owner, today.plusDays(7));
        completedOneTime.markAsCompleted("member");

        OneTimeTask expiredOneTime = new OneTimeTask("Overdue once", "", owner, today.minusDays(1));
        OneTimeTask dueToday = new OneTimeTask("Due today", "", owner, today);
        OneTimeTask futureOneTime = new OneTimeTask("Due later", "", owner, today.plusDays(1));

        RecurringTask completedRecurring = new RecurringTask("Done weekly", "", owner, DayOfWeek.SUNDAY);
        completedRecurring.markAsCompleted("member");
        RecurringTask openRecurring = new RecurringTask("Open weekly", "", owner, DayOfWeek.MONDAY);

        when(taskRepository.findAll()).thenReturn(List.of(
                completedOneTime, expiredOneTime, dueToday,
                futureOneTime, completedRecurring, openRecurring));

        // Production currently calls LocalDate.now() directly. Fix only this
        // test's date until production accepts an injected Clock.
        try (MockedStatic<LocalDate> dates = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            dates.when(LocalDate::now).thenReturn(today);
            maintenanceService.deleteAllCompletedOneTimeTasks();
        }

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
        TaskMaintenanceService maintenanceService = new TaskMaintenanceService(taskRepository);
        AppUser owner = new AppUser("member", "unused");

        RecurringTask completedRecurring = new RecurringTask("Weekly", "", owner, DayOfWeek.SUNDAY);
        completedRecurring.markAsCompleted("member");

        OneTimeTask completedOneTime = new OneTimeTask("Once", "", owner, LocalDate.of(2026, 11, 15));
        completedOneTime.markAsCompleted("member");

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
