package com.familyhome.security;

import com.familyhome.exception.UnauthorizedOperationException;
import com.familyhome.member.AppUser;
import com.familyhome.member.AppUserRepository;
import com.familyhome.member.Role;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("auth")
public class AuthService {

    private final AppUserRepository userRepository;

    public AuthService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isDriver(Authentication authentication) {
        return currentUser(authentication).map(AppUser::isDriver).orElse(false);
    }

    public boolean canManageTasks(Authentication authentication) {
        return currentUser(authentication)
                .map(user -> user.getRole() == Role.ADMIN || user.isTaskManager())
                .orElse(false);
    }

    private Optional<AppUser> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName());
    }

    public AppUser requireCurrentUser() {
        return currentUser(SecurityContextHolder.getContext().getAuthentication())
                .orElseThrow(
                        () -> new UnauthorizedOperationException("Authenticated user not found"));
    }
}
