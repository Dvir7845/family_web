package com.familyhome.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginRateLimiterTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private LoginRateLimiter loginRateLimiter;

    @Test
    void blocksAfterFiveFailuresAndUnblocksAfterCooldownOrSuccess() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-26T10:00:00Z"));
        Clock mutableClock =
                new Clock() {
                    @Override
                    public ZoneId getZone() {
                        return ZoneId.of("UTC");
                    }

                    @Override
                    public Clock withZone(ZoneId zone) {
                        return this;
                    }

                    @Override
                    public Instant instant() {
                        return now.get();
                    }
                };

        LoginRateLimiter limiter = new LoginRateLimiter(mutableClock, 5, Duration.ofMinutes(5), 3);

        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("alice");
            assertFalse(limiter.isBlocked("alice"));
        }

        limiter.recordFailure("alice");
        assertTrue(limiter.isBlocked("alice"));

        now.set(now.get().plus(Duration.ofMinutes(4).plusSeconds(59)));
        assertTrue(limiter.isBlocked("alice"));

        now.set(now.get().plusSeconds(1));
        assertFalse(limiter.isBlocked("alice"));

        limiter.recordFailure("bob");
        limiter.recordSuccess("bob");
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("bob");
        }
        assertFalse(limiter.isBlocked("bob"));
    }

    @Test
    void evictsOldestEntriesWhenCacheLimitIsReached() {
        LoginRateLimiter limiter =
                new LoginRateLimiter(Clock.systemUTC(), 5, Duration.ofMinutes(5), 2);

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("user1");
        }
        assertTrue(limiter.isBlocked("user1"));

        limiter.recordFailure("user2");
        limiter.recordFailure("user3");

        assertFalse(limiter.isBlocked("user1"));
    }

    @Test
    void locksOutAccountAfterRepeatedBadCredentialsViaHttp() throws Exception {
        try {
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(get("/me").with(httpBasic("admin", "WrongPassword!")))
                        .andExpect(status().isUnauthorized());
            }

            // Even with the right password, the 6th attempt is rejected during cooldown.
            mockMvc.perform(get("/me").with(httpBasic("admin", "TestBootstrap123!")))
                    .andExpect(status().isUnauthorized());
        } finally {
            loginRateLimiter.recordSuccess("admin");
        }
    }
}
