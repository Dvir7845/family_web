package com.familyhome.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familyhome.member.AppUser;
import com.familyhome.member.AppUserRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class TaskMaintenanceServiceTest {

    @Autowired private TaskRepository taskRepository;
    @Autowired private AppUserRepository userRepository;

    @Test
    void deletesCompletedOrExpiredOneTimeTasksAndKeepsTodayAndFutureTasks() {
        // 2030-03-09 22:30 UTC is already 2030-03-10 00:30 in Asia/Jerusalem (UTC+2).
        Clock clock =
                Clock.fixed(Instant.parse("2030-03-09T22:30:00Z"), ZoneId.of("Asia/Jerusalem"));
        TaskMaintenanceService maintenanceService =
                new TaskMaintenanceService(taskRepository, clock);
        AppUser owner = userRepository.saveAndFlush(new AppUser("member", "unused"));
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

        taskRepository.saveAllAndFlush(
                List.of(
                        completedOneTime,
                        expiredOneTime,
                        dueToday,
                        futureOneTime,
                        completedRecurring,
                        openRecurring));

        maintenanceService.deleteAllCompletedOneTimeTasks();

        Set<Long> remainingIds =
                taskRepository.findAll().stream().map(BaseTask::getId).collect(Collectors.toSet());

        assertEquals(
                Set.of(
                        dueToday.getId(),
                        futureOneTime.getId(),
                        completedRecurring.getId(),
                        openRecurring.getId()),
                remainingIds);
    }

    @Test
    void resetsOnlyRecurringTaskCompletionStatus() {
        TaskMaintenanceService maintenanceService =
                new TaskMaintenanceService(taskRepository, Clock.systemUTC());
        AppUser owner = userRepository.saveAndFlush(new AppUser("member2", "unused"));

        RecurringTask completedRecurring = new RecurringTask("Weekly", "", owner, DayOfWeek.SUNDAY);
        completedRecurring.markAsCompleted("member2", Instant.parse("2026-11-15T12:00:00Z"));

        OneTimeTask completedOneTime =
                new OneTimeTask("Once", "", owner, LocalDate.of(2026, 11, 15));
        completedOneTime.markAsCompleted("member2", Instant.parse("2026-11-15T12:00:00Z"));

        taskRepository.saveAllAndFlush(List.of(completedRecurring, completedOneTime));

        maintenanceService.resetWeeklyTaskCompletionStatus();

        BaseTask reloadedRecurring =
                taskRepository.findById(completedRecurring.getId()).orElseThrow();
        assertFalse(reloadedRecurring.isCompleted());
        assertNull(reloadedRecurring.getCompletedAt());
        assertNull(reloadedRecurring.getCompletedByUsername());

        BaseTask reloadedOneTime = taskRepository.findById(completedOneTime.getId()).orElseThrow();
        assertTrue(reloadedOneTime.isCompleted());
        assertNotNull(reloadedOneTime.getCompletedAt());
        assertEquals("member2", reloadedOneTime.getCompletedByUsername());
    }
}
