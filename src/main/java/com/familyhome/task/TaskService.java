package com.familyhome.task;

import com.familyhome.exception.BusinessRuleException;
import com.familyhome.exception.ResourceNotFoundException;
import com.familyhome.exception.UnauthorizedOperationException;
import com.familyhome.member.AppUser;
import com.familyhome.member.AppUserRepository;
import com.familyhome.member.AssigneeOption;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final AppUserRepository userRepository;
    private final Clock clock;

    public TaskService(
            TaskRepository taskRepository, AppUserRepository userRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    private AppUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new UnauthorizedOperationException("Authentication required");
        }

        return userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() -> new UnauthorizedOperationException("User not found"));
    }

    public Page<TaskResponse> getTasks(Pageable pageable) {
        return taskRepository.findAll(pageable).map(TaskResponse::from);
    }

    public BaseTask getTaskById(long id) {
        return taskRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Task not found"));
    }

    @Transactional
    @PreAuthorize("@auth.canManageTasks(authentication)")
    public List<TaskResponse> createTask(TaskRequest request) {
        validateRequest(request, false);

        AppUser user = findAssignee(request.assigneeId());

        return switch (request.type()) {
            case ONETIME -> {
                OneTimeTask task =
                        new OneTimeTask(
                                request.name(),
                                request.description(),
                                user,
                                request.taskDate());
                yield List.of(TaskResponse.from(taskRepository.save(task)));
            }
            case WEEKLY -> {
                List<RecurringTask> tasks =
                        request.days().stream()
                                .distinct()
                                .map(
                                        day ->
                                                new RecurringTask(
                                                        request.name(),
                                                        request.description(),
                                                        user,
                                                        day))
                                .toList();
                yield taskRepository.saveAll(tasks).stream().map(TaskResponse::from).toList();
            }
        };
    }

    @Transactional
    @PreAuthorize("@auth.canManageTasks(authentication)")
    public TaskResponse updateTask(long id, TaskRequest request) {
        validateRequest(request, true);
        BaseTask task = getTaskById(id);
        AppUser user = findAssignee(request.assigneeId());

        DayOfWeek day =
                (request.days() == null || request.days().isEmpty())
                        ? null
                        : request.days().getFirst();
        task.updateDetails(request.name(), request.description(), request.taskDate(), day);
        task.assignOwner(user);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    @PreAuthorize("@auth.canManageTasks(authentication)")
    public void deleteTask(long id) {
        BaseTask task = getTaskById(id);
        taskRepository.delete(task);
    }

    @Transactional
    public TaskResponse completeTask(long id) {
        AppUser user = getCurrentUser();

        BaseTask task = taskRepository
                .findByIdAndOwnerId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found among your assignments"));

        task.markAsCompleted(user.getUsername(), Instant.now(clock));

        return TaskResponse.from(taskRepository.save(task));
    }

    private AppUser findAssignee(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new BusinessRuleException("Selected assignee does not exist"));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@auth.canManageTasks(authentication)")
    public List<AssigneeOption> getAssignees() {
        return userRepository.findAll().stream()
                .map(user -> new AssigneeOption(user.getId(), user.getUsername()))
                .toList();
    }

    private void validateRequest(TaskRequest request, boolean singleDayRequired) {
        boolean hasDays = request.days() != null && !request.days().isEmpty();
        if (request.type() == TaskType.ONETIME && request.taskDate() == null) {
            throw new BusinessRuleException("Task date is required for one-time tasks");
        }
        if (request.type() == TaskType.WEEKLY && !hasDays) {
            throw new BusinessRuleException("At least one day of the week is required for weekly tasks");
        }
        if (request.type() == TaskType.WEEKLY && singleDayRequired && request.days().size() != 1) {
            throw new BusinessRuleException("Exactly one day of the week is required when updating a weekly task");
        }
        if (request.type() == TaskType.WEEKLY && request.taskDate() != null) {
            throw new BusinessRuleException("Task date should not be provided for weekly tasks");
        }
        if (request.type() == TaskType.ONETIME && hasDays) {
            throw new BusinessRuleException("Days of the week should not be provided for one-time tasks");
        }
    }
}
