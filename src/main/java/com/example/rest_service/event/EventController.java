package com.example.rest_service.event;

import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/events")
    public List<Event> getEvents() {
        return eventService.getEvents();
    }

    @GetMapping("/events/{id}")
    public Event getEventById(@PathVariable long id) {
        return eventService.getEventById(id);
    }

    @PostMapping("/events")
    public Event createEvent(@RequestBody @Valid EventRequest request) {
        return eventService.createEvent(request);
    }

    @PutMapping("/events/{id}")
    public Event updateEvent(
            @PathVariable long id,
            @RequestBody @Valid EventRequest request) {
        return eventService.updateEvent(id, request);
    }

    @DeleteMapping("/events/{id}")
    public void deleteEvent(@PathVariable long id) {
        eventService.deleteEvent(id);
    }

    @PostMapping("/events/{eventId}/invite/{userId}")
    public void addInvitedUser(
            @PathVariable long eventId,
            @PathVariable Long userId) {
        eventService.addInvitedUser(eventId, userId);
    }

}
