package com.familyhome.task;

import java.time.LocalDate;

import com.familyhome.member.AppUser;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;


@Entity
@DiscriminatorValue("ONETIME")
public class OneTimeTask extends BaseTask {

    @Column(name = "task_date")
    private LocalDate taskDate;

    protected OneTimeTask() {
    }

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

    public void updateDetails(String name, String description, LocalDate taskDate) {
        super.updateDetails(name, description);
        this.taskDate = taskDate;
    }

    @Override
    public boolean shouldDeleteOnDailyCleanup() {
        return isCompleted() || taskDate.isBefore(java.time.LocalDate.now()); //
    }
}
