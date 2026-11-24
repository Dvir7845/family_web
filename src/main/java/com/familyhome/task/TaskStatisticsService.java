package com.familyhome.task;

import com.familyhome.member.AppUserRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskStatisticsService {
    private final TaskRepository tasks;
    private final AppUserRepository users;
    private final Clock clock;

    public TaskStatisticsService(TaskRepository tasks, AppUserRepository users, Clock clock) {
        this.tasks = tasks;
        this.users = users;
        this.clock = clock;
    }

    public record MemberCount(Long id, String username, long completedCount) {}

    public record WeeklyStatistics(
            LocalDate weekStart, LocalDate weekEnd, List<MemberCount> members) {}

    @Transactional(readOnly = true)
    public WeeklyStatistics weekly() {
        ZoneId zone = ZoneId.of("Asia/Jerusalem");
        LocalDate start =
                LocalDate.now(clock.withZone(zone))
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        Map<String, Long> counts = new HashMap<>();
        tasks
                .findByCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                        start.atStartOfDay(zone).toInstant(),
                        start.plusWeeks(1).atStartOfDay(zone).toInstant())
                .stream()
                .filter(BaseTask::isCompleted)
                .forEach(task -> {
                    if (task.getCompletedByUsername() != null)
                        counts.merge(task.getCompletedByUsername(), 1L, Long::sum);
                });
        List<MemberCount> members =
                users.findAll().stream()
                        .map(user ->
                                new MemberCount(
                                        user.getId(),
                                        user.getUsername(),
                                        counts.getOrDefault(user.getUsername(), 0L)))
                        .sorted(
                                Comparator.comparingLong(MemberCount::completedCount)
                                        .reversed()
                                        .thenComparing(MemberCount::username))
                        .toList();
        return new WeeklyStatistics(start, start.plusDays(6), members);
    }
}
