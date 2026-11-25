package com.familyhome.event;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Events", description = "Family events and invitee management")
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/events")
    public Page<EventResponse> getEvents(
            @PageableDefault(size = 50, sort = "eventDate", direction = Sort.Direction.ASC)
                    Pageable pageable) {
        return eventService.getEvents(pageable);
    }

    @GetMapping("/events/{id}")
    public EventResponse getEventById(@PathVariable long id) {
        return eventService.getEventById(id);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(@RequestBody @Valid EventRequest request) {
        return eventService.createEvent(request);
    }

    @PutMapping("/events/{id}")
    public EventResponse updateEvent(
            @PathVariable long id, @RequestBody @Valid EventRequest request) {
        return eventService.updateEvent(id, request);
    }

    @DeleteMapping("/events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@PathVariable long id) {
        eventService.deleteEvent(id);
    }

    @PostMapping("/events/{eventId}/invite/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addInvitedUser(@PathVariable long eventId, @PathVariable Long userId) {
        eventService.addInvitedUser(eventId, userId);
    }
}
