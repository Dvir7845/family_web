package com.familyhome.member;

import java.nio.charset.StandardCharsets;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MemberService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AssigneeOption createMember(CreateMemberRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password exceeds the supported byte length");
        }

        AppUser user = new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()));

        user.changeRole(Role.MEMBER);
        user.changeDriver(Boolean.TRUE.equals(request.driver()));
        user.changeTaskManager(Boolean.TRUE.equals(request.taskManager()));

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Could not create member: conflicting account data");
        }

        return new AssigneeOption(user.getId(), user.getUsername(), user.isDriver(), user.isTaskManager());
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void changeTaskManager(Long userId, Boolean taskManager) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));
        user.changeTaskManager(taskManager);
    }

    @Transactional(readOnly = true)
    public CurrentMemberResponse getCurrentMember(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"));

        return new CurrentMemberResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(), user.isDriver(), user.isTaskManager());
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void changeDriver(Long id, boolean driver) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.changeDriver(driver);
    }

    @Transactional(readOnly = true)
    public List<AssigneeOption> getMembers() {
        return userRepository.findAll()
                .stream()
                .map(user -> new AssigneeOption(
                        user.getId(),
                        user.getUsername(),
                        user.isDriver(),
                        user.isTaskManager(),
                        user.getRole()))
                .toList();
    }
}