package com.familyhome.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<BaseTask, Long> {

    List<BaseTask> findByCompletedAtGreaterThanEqualAndCompletedAtLessThan(
            Instant start, Instant end);

    List<BaseTask> findAllByOwnerId(Long ownerId);

    Optional<BaseTask> findByIdAndOwnerId(Long id, Long ownerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "DELETE FROM OneTimeTask t WHERE t.completed = true OR (t.taskDate IS NOT NULL AND t.taskDate < :today)")
    int deleteCompletedOrExpiredOneTimeTasks(@Param("today") LocalDate today);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "UPDATE RecurringTask t SET t.completed = false, t.completedAt = null, t.completedByUsername = null")
    int resetAllRecurringTasks();
}
