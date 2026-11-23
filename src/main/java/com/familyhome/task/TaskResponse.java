package com.familyhome.task;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
     Long id,
     String name,
     String description,
     TaskType type,
     LocalDate taskDate,
     DayOfWeek day,
     Long assigneeId,
     String assigneeUsername,
     boolean completed,
     Instant completedAt,
     String completedByUsername) {

    public static TaskResponse from(BaseTask task) {
        return new TaskResponse(
            task.getId(),
            task.getName(),
            task.getDescription(),
            task.getType(),
            task.getTaskDate(),
            task.getDay(),
            task.getAssigneeId(),
            task.getAssigneeUsername(),
            task.isCompleted(),
            task.getCompletedAt(),
            task.getCompletedByUsername()
        );
    }
    
}
