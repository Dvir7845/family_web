package com.familyhome.member;

import java.nio.charset.StandardCharsets;
import com.familyhome.task.TaskRepository;
import com.familyhome.event.EventRepository;
import com.familyhome.vehicle.VehicleBookingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {
    private final AppUserRepository users;
    private final TaskRepository tasks;
    private final EventRepository events;
    private final VehicleBookingRepository bookings;
    private final PasswordEncoder passwords;
    private final SessionRegistry sessions;
    public AccountService(AppUserRepository users, TaskRepository tasks, EventRepository events,
            VehicleBookingRepository bookings, PasswordEncoder passwords, SessionRegistry sessions) {
        this.users = users; this.tasks = tasks; this.events = events;
        this.bookings = bookings; this.passwords = passwords; this.sessions = sessions;
    }
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        AppUser user = users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (request.currentPassword().getBytes(StandardCharsets.UTF_8).length > 72 ||
                !passwords.matches(request.currentPassword(), user.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password exceeds the supported byte length");
        user.changePassword(passwords.encode(request.newPassword()));
        expireAfterCommit(username);
    }
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id, String currentUsername) {
        AppUser user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getUsername().equals(currentUsername))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete your own account");
        tasks.findAllByOwnerId(id).forEach(task -> task.assignOwner(null));
        events.findAll().forEach(event -> {
            if (event.getInvitedUserIds().contains(id)) {
                var invitees = new java.util.ArrayList<>(event.getInvitedUserIds());
                invitees.removeIf(id::equals);
                event.updateDetails(event.getDescription(), event.getEventDate(), invitees);
            }
        });
        bookings.deleteByUserId(id);
        users.delete(user);
        expireAfterCommit(user.getUsername());
    }
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void promote(Long id) {
        AppUser user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() == Role.ADMIN) return;
        user.changeRole(Role.ADMIN);
        expireAfterCommit(user.getUsername());
    }

    private void expireAfterCommit(String username) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                sessions.getAllPrincipals().stream()
                    .filter(p -> p instanceof UserDetails details && details.getUsername().equals(username))
                    .forEach(p -> sessions.getAllSessions(p, false).forEach(session -> session.expireNow()));
            }
        });
    }
}
