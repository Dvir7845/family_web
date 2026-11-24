package com.familyhome.task;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskMaintenanceService {

    private final TaskRepository taskRepository;
    private final Clock clock;

    public TaskMaintenanceService(TaskRepository taskRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    @Transactional
    public void deleteAllTasks() {
        taskRepository.deleteAll();
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Jerusalem") // Every day at midnight
    public void deleteAllCompletedOneTimeTasks() {
        LocalDate today = LocalDate.now(clock);
        for (BaseTask task : taskRepository.findAll()) {
            if (task.shouldDeleteOnDailyCleanup(today)) {
                taskRepository.delete(task);
            }
        }
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * SUN", zone = "${app.time-zone:Asia/Jerusalem}") // Every Sunday at midnight
    public void resetWeeklyTaskCompletionStatus() {
        for (BaseTask task : taskRepository.findAll()) {
            task.resetWeeklyStatus();
        }
    }
}
