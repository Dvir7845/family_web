package com.familyhome.member;

import com.familyhome.event.EventRepository;
import com.familyhome.exception.BusinessRuleException;
import com.familyhome.exception.ForbiddenOperationException;
import com.familyhome.exception.ResourceNotFoundException;
import com.familyhome.exception.UnauthorizedOperationException;
import com.familyhome.task.TaskRepository;
import com.familyhome.vehicle.VehicleBookingRepository;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AccountService {
    private final AppUserRepository users;
    private final TaskRepository tasks;
    private final EventRepository events;
    private final VehicleBookingRepository bookings;
    private final PasswordEncoder passwords;
    private final SessionRegistry sessions;
    private final boolean demoEnabled;
    private final String demoUsername;

    public AccountService(
            AppUserRepository users,
            TaskRepository tasks,
            EventRepository events,
            VehicleBookingRepository bookings,
            PasswordEncoder passwords,
            SessionRegistry sessions) {
        this(users, tasks, events, bookings, passwords, sessions, false, "demo");
    }

    @Autowired
    public AccountService(
            AppUserRepository users,
            TaskRepository tasks,
            EventRepository events,
            VehicleBookingRepository bookings,
            PasswordEncoder passwords,
            SessionRegistry sessions,
            @Value("${app.demo.enabled:false}") boolean demoEnabled,
            @Value("${app.demo.username:demo}") String demoUsername) {
        this.users = users;
        this.tasks = tasks;
        this.events = events;
        this.bookings = bookings;
        this.passwords = passwords;
        this.sessions = sessions;
        this.demoEnabled = demoEnabled;
        this.demoUsername = demoUsername;
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        if (demoEnabled && demoUsername.equals(username)) {
            throw new ForbiddenOperationException("Cannot change the demo account password");
        }
        AppUser user =
                users.findByUsername(username)
                        .orElseThrow(() -> new UnauthorizedOperationException("User not found"));
        if (request.currentPassword().getBytes(StandardCharsets.UTF_8).length > 72
                || !passwords.matches(request.currentPassword(), user.getPasswordHash()))
            throw new BusinessRuleException("Current password is incorrect");
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new BusinessRuleException("Password exceeds the supported byte length");
        user.changePassword(passwords.encode(request.newPassword()));
        expireAfterCommit(username);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id, String currentUsername) {
        AppUser user =
                users.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getUsername().equals(currentUsername))
            throw new BusinessRuleException("Cannot delete your own account");
        if (demoEnabled && demoUsername.equals(user.getUsername())) {
            throw new ForbiddenOperationException("Cannot delete the demo account");
        }
        tasks.findAllByOwnerId(id).forEach(task -> task.assignOwner(null));
        events.deleteInviteeByUserId(id);
        bookings.deleteByUserId(id);
        users.delete(user);
        expireAfterCommit(user.getUsername());
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void promote(Long id) {
        AppUser user =
                users.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == Role.ADMIN) return;
        user.changeRole(Role.ADMIN);
        expireAfterCommit(user.getUsername());
    }

    private void expireAfterCommit(String username) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        sessions.getAllPrincipals().stream()
                                .filter(p ->
                                        p instanceof UserDetails details
                                                && details.getUsername().equals(username))
                                .forEach(p ->
                                        sessions.getAllSessions(p, false)
                                                .forEach(session -> session.expireNow()));
                    }
                });
    }
}
