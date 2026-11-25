package com.familyhome.event;

import com.familyhome.member.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @BatchSize(size = 50)
    @ManyToMany
    @JoinTable(
            name = "event_invitees",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<AppUser> invitedUsers = new LinkedHashSet<>();

    protected Event() {}

    public Event(String description, LocalDate eventDate, Set<AppUser> invitedUsers) {
        this.description = description;
        this.eventDate = eventDate;
        this.invitedUsers =
                invitedUsers == null ? new LinkedHashSet<>() : new LinkedHashSet<>(invitedUsers);
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

    public Set<AppUser> getInvitedUsers() {
        return invitedUsers;
    }

    public void addInvitedUser(AppUser user) {
        this.invitedUsers.add(user);
    }

    public void removeInvitedUser(AppUser user) {
        this.invitedUsers.remove(user);
    }

    public void updateDetails(String description, LocalDate eventDate, Set<AppUser> invitedUsers) {
        this.description = description;
        this.eventDate = eventDate;
        this.invitedUsers = invitedUsers;
    }
}
