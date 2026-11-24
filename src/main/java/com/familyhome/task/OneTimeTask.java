package com.familyhome.task;

import com.familyhome.exception.BusinessRuleException;
import com.familyhome.member.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Entity
@DiscriminatorValue("ONETIME")
public class OneTimeTask extends BaseTask {

    @Column(name = "task_date")
    private LocalDate taskDate;

    protected OneTimeTask() {}

    public OneTimeTask(String name, String description, AppUser owner, LocalDate taskDate) {
        super(name, description, owner);
        this.taskDate = taskDate;
    }

    public LocalDate getTaskDate() {
        return taskDate;
    }

    public void setTaskDate(LocalDate taskDate) {
        this.taskDate = taskDate;
    }

    @Override
    public void updateDetails(String name, String description, LocalDate taskDate, DayOfWeek day) {
        if (taskDate == null || day != null) {
            throw new BusinessRuleException("Cannot change task type");
        }
        this.taskDate = taskDate;
        updateDetails(name, description);
    }

    @Override
    public boolean shouldDeleteOnDailyCleanup(LocalDate today) {
        return isCompleted() || (taskDate != null && taskDate.isBefore(today));
    }

    @Override
    public TaskType getType() {
        return TaskType.ONETIME;
    }
}
