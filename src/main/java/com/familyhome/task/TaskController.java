package com.familyhome.task;

import com.familyhome.member.AssigneeOption;
import jakarta.validation.Valid;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class TaskController {

    private final TaskService taskService;
    private final TaskStatisticsService statistics;

    public TaskController(TaskService taskService, TaskStatisticsService statistics) {
        this.taskService = taskService;
        this.statistics = statistics;
    }

    @GetMapping("/tasks/statistics/weekly")
    public TaskStatisticsService.WeeklyStatistics weeklyStatistics() {
        return statistics.weekly();
    }

    @GetMapping("/tasks")
    public Page<TaskResponse> getTasks(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.ASC)
                    Pageable pageable) {
        return taskService.getTasks(pageable);
    }

    @GetMapping("/tasks/{id}")
    public TaskResponse getTaskById(@PathVariable long id) {
        return TaskResponse.from(taskService.getTaskById(id));
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@Valid @RequestBody TaskRequest request) { 
        return taskService.createTask(request);
    }

    @PutMapping("/tasks/{id}")
    public TaskResponse updateTask(@PathVariable long id, @Valid @RequestBody TaskRequest request) {
        return taskService.updateTask(id, request);
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable long id) {
        taskService.deleteTask(id);
    }

    @PatchMapping("/tasks/{id}/complete")
    public TaskResponse completeTask(@PathVariable long id) {
        return taskService.completeTask(id);
    }

    @GetMapping("/tasks/assignees")
    public List<AssigneeOption> getAssignees() {
        return taskService.getAssignees();
    }
}
