package com.familyhome.event;

import com.familyhome.member.AppUser;
import java.time.LocalDate;
import java.util.List;

public record EventResponse(
        Long id, String description, LocalDate eventDate, List<Long> invitedUserIds) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getDescription(),
                event.getEventDate(),
                event.getInvitedUser().stream().map(AppUser::getId).toList());
    }
}
