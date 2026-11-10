package com.familyhome.event;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.LocalDate;

import java.util.List;


@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "invited_user_id", nullable = false)
    private List<Long> invitedUserIds;

    protected Event() {
    }

    public Event(String description, LocalDate eventDate, List<Long> invitedUserIds) {
        this.description = description;
        this.eventDate = eventDate;
        this.invitedUserIds = invitedUserIds;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public List<Long> getInvitedUserIds() {
        return invitedUserIds;
    }

    public void addInvitedUserId(Long userId) {
        this.invitedUserIds.add(userId);
    }

    public void removeInvitedUserId(Long userId) {
        this.invitedUserIds.remove(userId);
    }

    public void updateDetails(String description, LocalDate eventDate, List<Long> invitedUserIds) {
        this.description = description;
        this.eventDate = eventDate;
        this.invitedUserIds = invitedUserIds;
    }
}