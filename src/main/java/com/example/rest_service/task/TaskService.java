package com.example.rest_service.task;

import com.example.rest_service.member.AppUser;
import com.example.rest_service.member.AppUserRepository;
import com.example.rest_service.member.AssigneeOption;
import com.example.rest_service.member.Role;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final AppUserRepository userRepository;

    public TaskService(
            TaskRepository taskRepository,
            AppUserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    private AppUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"));
    }

    public List<Task> getTasks() {
        return taskRepository.findAll();
    }

    public Task getTaskById(long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Task not found"));
    }

    @Transactional
    public Task createTask(TaskRequest request) {
        requireAdmin();
        
        AppUser user = findAssignee(request.assigneeId());

        Task task = new Task(
                request.name(),
                request.day(),
                request.description());

        task.assignOwner(user);
        return taskRepository.save(task);
    }

    @Transactional
    public Task updateTask(long id, TaskRequest request) {
        requireAdmin();

        Task task = getTaskById(id);
        AppUser assignee = findAssignee(request.assigneeId());

        task.updateDetails(
                request.name(),
                request.day(),
                request.description());

        task.assignOwner(assignee);
        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(long id) {
        requireAdmin();
        Task task = getTaskById(id);
        taskRepository.delete(task);
    }

    @Transactional
    public Task completeTask(long id) {
        AppUser user = getCurrentUser();

        Task task = taskRepository.findByIdAndOwnerId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Task not found among your assignments"));

        task.markCompleted();

        return taskRepository.save(task);
    }

    private AppUser requireAdmin() {
        AppUser user = getCurrentUser();

        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only an admin can manage tasks");
        }

        return user;
    }

    private AppUser findAssignee(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Selected assignee does not exist"));
    }

    @Transactional(readOnly = true)
    public List<AssigneeOption> getAssignees() {
        requireAdmin();

        return userRepository.findAll()
                .stream()
                .map(user -> new AssigneeOption(
                        user.getId(),
                        user.getUsername()))
                .toList();
    }
}