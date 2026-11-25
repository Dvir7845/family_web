package com.familyhome.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.familyhome.member.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class TaskStatisticsTest {
    @Test
    void weekStartsSundayInIsraelAndIncludesZeroCounts() {
        TaskRepository tasks = mock(TaskRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        Clock clock =
                Clock.fixed(Instant.parse("2026-11-07T22:30:00Z"), ZoneId.of("Asia/Jerusalem"));
        AppUser a = mock(AppUser.class), b = mock(AppUser.class);
        when(a.getId()).thenReturn(1L);
        when(a.getUsername()).thenReturn("alice");
        when(b.getId()).thenReturn(2L);
        when(b.getUsername()).thenReturn("bob");
        when(users.findAll()).thenReturn(List.of(a, b));
        OneTimeTask task = new OneTimeTask("Task", "", null, LocalDate.of(2026, 11, 8));
        task.markAsCompleted("alice", Instant.parse("2026-11-08T10:00:00Z"));
        Instant start = Instant.parse("2026-11-07T22:00:00Z"),
                end = Instant.parse("2026-11-14T22:00:00Z");
        when(tasks.findByCompletedAtGreaterThanEqualAndCompletedAtLessThan(start, end))
                .thenReturn(List.of(task));
        var result = new TaskStatisticsService(tasks, users, clock).weekly();
        assertEquals(LocalDate.of(2026, 11, 8), result.weekStart());
        assertEquals(LocalDate.of(2026, 11, 14), result.weekEnd());
        assertEquals(1, result.members().get(0).completedCount());
        assertEquals(0, result.members().get(1).completedCount());
        verify(tasks).findByCompletedAtGreaterThanEqualAndCompletedAtLessThan(start, end);
    }

    @Test
    void repeatedCompletionAndReassignmentPreserveOriginalCredit() {
        OneTimeTask task = new OneTimeTask("Task", "", null, LocalDate.of(2026, 11, 8));
        Instant first = Instant.parse("2026-11-08T10:00:00Z");
        task.markAsCompleted("alice", first);
        task.assignOwner(new AppUser("bob", "unused"));
        task.markAsCompleted("bob", Instant.parse("2026-11-08T11:00:00Z"));
        assertEquals(first, task.getCompletedAt());
        assertEquals("alice", task.getCompletedByUsername());
    }
}
