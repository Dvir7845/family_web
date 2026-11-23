package com.familyhome.task;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<BaseTask, Long> {

    List<BaseTask> findByCompletedAtGreaterThanEqualAndCompletedAtLessThan(
            Instant start, Instant end);

    List<BaseTask> findAllByOwnerId(Long ownerId);

    Optional<BaseTask> findByIdAndOwnerId(Long id, Long ownerId);
}
