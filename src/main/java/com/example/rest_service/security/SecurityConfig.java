package com.example.rest_service.security;

import com.example.rest_service.member.AppUser;
import com.example.rest_service.member.AppUserRepository;
import com.example.rest_service.member.Role;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            AppUserRepository userRepository) {
        return username -> {
            AppUser appUser = userRepository.findByUsername(username)
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
            Environment environment) {
        return args -> {
            if (userRepository.findByUsername("dvir").isPresent()) {
                return;
            }

            String password = environment.getProperty("APP_LOGIN_PASSWORD");

            if (password == null || password.isBlank()) {
                throw new IllegalStateException(
                        "Set APP_LOGIN_PASSWORD to create the initial user");
            }

            AppUser user = new AppUser(
                    "dvir",
                    passwordEncoder.encode(password));
                    
            user.changeRole(Role.ADMIN);
            userRepository.save(user);
        };
    }
}