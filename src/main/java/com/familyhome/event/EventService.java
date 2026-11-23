package com.familyhome.event;

import com.familyhome.exception.ResourceNotFoundException;
import com.familyhome.exception.BusinessRuleException;
import com.familyhome.member.AppUser;
import com.familyhome.member.AppUserRepository;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
public class EventService {
    private final EventRepository eventRepository;
    private final AppUserRepository userRepository;

    public EventService(EventRepository eventRepository, AppUserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }
    
    @Transactional (readOnly = true)
    public Page<EventResponse> getEvents(Pageable pageable) {
        return eventRepository.findAll(pageable).map(EventResponse::from);
    }

    @Transactional (readOnly = true)
    public EventResponse getEventById(long id) {
        return eventRepository
                .findById(id)
                .map(EventResponse::from)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Event not found"));
    }

    @Transactional
    public EventResponse createEvent(EventRequest request) {
        Set<AppUser> invitedUsers = resolveInvitedUsers(request.invitedUserIds());
        Event event = new Event(request.description(), request.eventDate(), invitedUsers);
        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    public EventResponse updateEvent(long id, EventRequest request) {
        resolveInvitedUsers(request.invitedUserIds());
        Event event =
                eventRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Event not found"));
        
        Set<AppUser> invitedUsers = resolveInvitedUsers(request.invitedUserIds());
        event.updateDetails(request.description(), request.eventDate(), invitedUsers);

        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    public void deleteEvent(long id) {
        Event event =
                eventRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Event not found"));

        eventRepository.delete(event);
    }

    @Transactional
    public void addInvitedUser(long eventId, Long userId) {
        Event event =
                eventRepository
                        .findById(eventId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Event not found"));

        if (userId == null || !userRepository.existsById(userId)) {
            throw new BusinessRuleException("Invited user does not exist");
        }
        AppUser user = userRepository.findById(userId).get();
        event.addInvitedUser(user);
        eventRepository.save(event);
    }

    private Set<AppUser> resolveInvitedUsers(List<Long> invitedUserIds) {
        Set<AppUser> invitedUsers = new LinkedHashSet<>();
        for (Long userId : invitedUserIds) {
            AppUser user = userRepository.findById(userId).orElseThrow(() -> 
                new BusinessRuleException("Invited user with ID " + userId + " does not exist"));
            invitedUsers.add(user);
        }
        return invitedUsers;
    }
}
