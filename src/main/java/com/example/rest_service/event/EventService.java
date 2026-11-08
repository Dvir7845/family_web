package com.example.rest_service.event;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.example.rest_service.member.AppUserRepository;

import java.util.List;

@Service
public class EventService {
    private final EventRepository eventRepository;
    private final AppUserRepository userRepository;

    public EventService(EventRepository eventRepository, AppUserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    public List<Event> getEvents() {
        return eventRepository.findAll();
    }

    public Event getEventById(long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    @Transactional
    public Event createEvent(EventRequest request) {
        validateInvitedUsers(request.invitedUserIds());
        Event event = new Event(
                request.description(),
                request.eventDate(),
                request.invitedUserIds());
        return eventRepository.save(event);
    }

    @Transactional
    public Event updateEvent(long id, EventRequest request) {
        validateInvitedUsers(request.invitedUserIds());
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        event.updateDetails(request.description(), request.eventDate(), request.invitedUserIds());

        return eventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        eventRepository.delete(event);
    }

    @Transactional
    public void addInvitedUser(long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (userId == null || !userRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invited user does not exist");
        }
        event.addInvitedUserId(userId);
        eventRepository.save(event);
    }

    private void validateInvitedUsers(List<Long> userIds) {
        for (Long userId : userIds) {
            if (!userRepository.existsById(userId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invited user does not exist: " + userId);
            }
        }
    }
}
