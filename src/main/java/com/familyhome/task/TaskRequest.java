package com.familyhome.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TaskRequest(
        @NotBlank(message = "Task name is required") String name,

        @NotBlank(message = "Day is required") String day,

        String description,

        @NotNull(message = "Assignee is required") @Positive(message = "Assignee ID must be positive") Long assigneeId) {
}