package com.familyhome.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record TaskRequest(
        @NotBlank String name,
        String description,
        @NotNull @Positive Long assigneeId,
        @NotNull TaskType type,
        LocalDate taskDate,
        List<@NotNull DayOfWeek> days) {}
