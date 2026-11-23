package com.familyhome.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinColumn;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import com.familyhome.member.AppUser;



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

    @ManyToMany
    @JoinTable (
        name = "event_invitees",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<AppUser> invitedUser = new LinkedHashSet<>();

    protected Event() {}

    public Event(String description, LocalDate eventDate, Set<AppUser> invitedUser) {
        this.description = description;
        this.eventDate = eventDate;
        this.invitedUser = invitedUser == null ? new LinkedHashSet<>() : new LinkedHashSet<>(invitedUser);
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

    public Set<AppUser> getInvitedUser() {
        return invitedUser;
    }

    public void addInvitedUser(AppUser user) {
        this.invitedUser.add(user);
    }

    public void removeInvitedUser(AppUser user) {
        this.invitedUser.remove(user);
    }

    public void updateDetails(String description, LocalDate eventDate, Set<AppUser> invitedUserIds) {
        this.description = description;
        this.eventDate = eventDate;
        this.invitedUser = invitedUserIds;
    }
}
