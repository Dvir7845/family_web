package com.familyhome.task;

import com.familyhome.exception.BusinessRuleException;
import com.familyhome.member.AppUser;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Entity
@DiscriminatorValue("WEEKLY")
public class RecurringTask extends BaseTask {

    @Enumerated(EnumType.STRING)
    private DayOfWeek day;

    protected RecurringTask() {}

    public RecurringTask(String name, String description, AppUser owner, DayOfWeek day) {
        super(name, description, owner);
        this.day = day;
    }

    public DayOfWeek getDay() {
        return day;
    }

    @Override
    public void updateDetails(String name, String description, LocalDate taskDate, DayOfWeek day) {
        if (taskDate != null || day == null) {
            throw new BusinessRuleException("Cannot change task type");
        }
        super.updateDetails(name, description);
        this.day = day;
    }

    @Override
    public void resetWeeklyStatus() {
        super.setCompleted(false);
        super.setCompletedAt(null);
        super.setCompletedByUsername(null);
    }

    @Override
    public TaskType getType() {
        return TaskType.WEEKLY;
    }
}
