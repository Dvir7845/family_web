package com.familyhome.task;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;


@Service 
public class TaskMaintenanceService {

    private final TaskRepository taskRepository;

    public TaskMaintenanceService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional 
    public void deleteAllTasks() {
        taskRepository.deleteAll();
    }
 
    @Transactional
    @Scheduled (cron = "0 0 0 * * ?", zone = "Asia/Jerusalem") // Every day at midnight 
    public void deleteAllCompletedOneTimeTasks() {
        for (BaseTask task : taskRepository.findAll()) {
            if (task.shouldDeleteOnDailyCleanup()) {
                taskRepository.delete(task);
            }
        }
    }
    
    @Transactional
    @Scheduled(cron = "0 0 0 * * SUN" , zone = "Asia/Jerusalem")  // Every Sunday at midnight
    public void resetWeeklyTaskCompletionStatus() {
        for (BaseTask task : taskRepository.findAll()) {
            task.resetWeeklyStatus();
        }
    }
}