package com.familyhome.security;

import com.familyhome.member.AppUser;
import com.familyhome.member.AppUserRepository;
import com.familyhome.member.Role;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public LoginRateLimiter loginRateLimiter(ObjectProvider<Clock> clockProvider) {
        return new LoginRateLimiter(clockProvider.getIfAvailable(Clock::systemUTC));
    }

    @Bean
    public UserDetailsService userDetailsService(
            AppUserRepository userRepository, LoginRateLimiter loginRateLimiter) {
        return username -> {
            if (loginRateLimiter.isBlocked(username)) {
                throw new LockedException(
                        "Too many failed login attempts. Try again in a few minutes.");
            }
            AppUser appUser =
                    userRepository
                            .findByUsername(username)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            return User.withUsername(appUser.getUsername())
                    .password(appUser.getPasswordHash())
                    .roles(appUser.getRole().name())
                    .build();
        };
    }

    @Bean
    public CommandLineRunner createInitialUser(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            Environment environment,
            @Value("${app.admin.username:admin}") String adminUsername) {
        return args -> {
            if (userRepository.existsByRole(Role.ADMIN)) {
                return;
            }

            String password = environment.getProperty("APP_LOGIN_PASSWORD");

            if (password == null || password.isBlank()) {
                throw new IllegalStateException(
                        "Set APP_LOGIN_PASSWORD to create the initial user");
            }

            AppUser user = new AppUser(adminUsername, passwordEncoder.encode(password));

            user.changeRole(Role.ADMIN);
            userRepository.save(user);
        };
    }
}
