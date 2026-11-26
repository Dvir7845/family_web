package com.familyhome.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

public class LoginRateLimiter {

    static final int DEFAULT_MAX_ATTEMPTS = 5;
    static final Duration DEFAULT_LOCK_DURATION = Duration.ofMinutes(5);
    static final int DEFAULT_MAX_ENTRIES = 10_000;

    private final Clock clock;
    private final int maxAttempts;
    private final Duration lockDuration;
    private final Map<String, AttemptState> attempts;

    public LoginRateLimiter(Clock clock) {
        this(clock, DEFAULT_MAX_ATTEMPTS, DEFAULT_LOCK_DURATION, DEFAULT_MAX_ENTRIES);
    }

    LoginRateLimiter(Clock clock, int maxAttempts, Duration lockDuration, int maxEntries) {
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
        this.attempts =
                new LinkedHashMap<>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, AttemptState> eldest) {
                        return size() > maxEntries;
                    }
                };
    }

    public synchronized boolean isBlocked(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        AttemptState state = attempts.get(username);
        if (state == null || state.lockedUntil == null) {
            return false;
        }
        if (!clock.instant().isBefore(state.lockedUntil)) {
            attempts.remove(username);
            return false;
        }
        return true;
    }

    public synchronized void recordFailure(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        Instant now = clock.instant();
        AttemptState state = attempts.get(username);
        if (state == null || (state.lockedUntil != null && !now.isBefore(state.lockedUntil))) {
            state = new AttemptState();
            attempts.put(username, state);
        }
        state.failures++;
        if (state.failures >= maxAttempts) {
            state.lockedUntil = now.plus(lockDuration);
        }
    }

    public synchronized void recordSuccess(String username) {
        if (username != null) {
            attempts.remove(username);
        }
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        recordSuccess(event.getAuthentication().getName());
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if (principal instanceof String username) {
            recordFailure(username);
        } else {
            recordFailure(event.getAuthentication().getName());
        }
    }

    private static final class AttemptState {
        int failures;
        Instant lockedUntil;
    }
}
