package com.familyhome.task;

import com.familyhome.member.AssigneeOption;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
public class TaskController {

    private final TaskService taskService;
    private final TaskStatisticsService statistics;

    public TaskController(TaskService taskService, TaskStatisticsService statistics) {
        this.taskService = taskService;
        this.statistics = statistics;
    }

    @GetMapping("/tasks/statistics/weekly")
    public TaskStatisticsService.WeeklyStatistics weeklyStatistics() { return statistics.weekly(); }

    @GetMapping("/tasks")
    public List<Task> getTasks() {
        return taskService.getTasks();
    }

    @GetMapping("/tasks/{id}")
    public Task getTaskById(@PathVariable long id) {
        return taskService.getTaskById(id);
    }

    @PostMapping("/tasks")
    public Task createTask(@Valid @RequestBody TaskRequest request) {
        return taskService.createTask(request);
    }

    @PutMapping("/tasks/{id}")
    public Task updateTask(
            @PathVariable long id,
            @Valid @RequestBody TaskRequest request) {
        return taskService.updateTask(id, request);
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable long id) {
        taskService.deleteTask(id);
    }

    @PatchMapping("/tasks/{id}/complete")
    public Task completeTask(@PathVariable long id) {
        return taskService.completeTask(id);
    }

    @GetMapping("/tasks/assignees")
    public List<AssigneeOption> getAssignees() {
        return taskService.getAssignees();
    }
}