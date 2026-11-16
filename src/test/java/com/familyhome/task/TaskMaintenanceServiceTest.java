package com.familyhome.task;

import com.familyhome.member.AppUser;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskMaintenanceServiceTest {

    @Test
    void deletesOnlyCompletedOneTimeTasks() {
        TaskRepository taskRepository = mock(TaskRepository.class);
        TaskMaintenanceService maintenanceService = new TaskMaintenanceService(taskRepository);
        AppUser owner = new AppUser("member", "unused");

        OneTimeTask completedOneTime = new OneTimeTask("Done once", "", owner, LocalDate.of(2026, 11, 15));
        completedOneTime.markAsCompleted("member");

        OneTimeTask openOneTime = new OneTimeTask("Open once", "", owner, LocalDate.of(2026, 11, 16));

        RecurringTask completedRecurring = new RecurringTask("Done weekly", "", owner, DayOfWeek.SUNDAY);
        completedRecurring.markAsCompleted("member");

        when(taskRepository.findAll()).thenReturn(List.of(completedOneTime, openOneTime, completedRecurring));

        maintenanceService.deleteAllCompletedOneTimeTasks();

        verify(taskRepository).delete(completedOneTime);
        verify(taskRepository, never()).delete(openOneTime);
        verify(taskRepository, never()).delete(completedRecurring);
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
