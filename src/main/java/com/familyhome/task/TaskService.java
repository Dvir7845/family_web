package com.familyhome.task;

import com.familyhome.member.AppUser;
import com.familyhome.member.AppUserRepository;
import com.familyhome.member.AssigneeOption;
import com.familyhome.member.Role;

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

    public List<BaseTask> getTasks() {
        return taskRepository.findAll();
    }

    public BaseTask getTaskById(long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Task not found"));
    }

    @Transactional
    public BaseTask createTask(TaskRequest request) {
        requireAdmin();
        validateRequest(request);

        AppUser user = findAssignee(request.assigneeId());

        BaseTask task = switch (request.type()) {
            case ONETIME -> new OneTimeTask(
                    request.name(),
                    request.description(),
                    user,
                    request.taskDate());
            case WEEKLY -> new RecurringTask(
                    request.name(),
                    request.description(),
                    user,
                    request.day());
        };
        return taskRepository.save(task);
    }

    @Transactional
    public BaseTask updateTask(long id, TaskRequest request) {
        requireAdmin();
        validateRequest(request);
        BaseTask task = getTaskById(id);
        AppUser user = findAssignee(request.assigneeId());

        if (task instanceof OneTimeTask oneTimeTask && request.type() == TaskType.ONETIME) {
            oneTimeTask.updateDetails(request.name(), request.description(), request.taskDate());
        } else if (task instanceof RecurringTask recurringTask && request.type() == TaskType.WEEKLY) {
            recurringTask.updateDetails(request.name(), request.description(), request.day());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change task type");
        }
        task.assignOwner(user);
        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(long id) {
        requireAdmin();
        BaseTask task = getTaskById(id);
        taskRepository.delete(task);
    }

    @Transactional
    public BaseTask completeTask(long id) {
        AppUser user = getCurrentUser();

        BaseTask task = taskRepository.findByIdAndOwnerId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Task not found among your assignments"));

        task.markAsCompleted(user.getUsername());

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

    private void validateRequest(TaskRequest request) {
        if (request.type() == TaskType.ONETIME && request.taskDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Task date is required for one-time tasks");
        }
        if (request.type() == TaskType.WEEKLY && request.day() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Day of the week is required for weekly tasks");
        }
        if (request.type() == TaskType.WEEKLY && request.taskDate() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Task date should not be provided for weekly tasks");
        }
        if (request.type() == TaskType.ONETIME && request.day() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Day of the week should not be provided for one-time tasks");
        }

    }
}