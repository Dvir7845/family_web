package com.familyhome.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;

public record EventRequest(
        @NotBlank(message = "Description is required") String description,
        @NotNull(message = "Event date is required") LocalDate eventDate,
        @NotNull(message = "Invited user IDs are required")
                List<
                                @NotNull(message = "Invited user ID is required")
                                @Positive(message = "Invited user ID must be positive") Long>
                        invitedUserIds) {}
