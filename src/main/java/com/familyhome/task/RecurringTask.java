package com.familyhome.task;

import com.familyhome.member.AppUser;

import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

import java.time.DayOfWeek;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;


@Entity
@DiscriminatorValue("WEEKLY")
public class RecurringTask extends BaseTask {
   
    @Enumerated(EnumType.STRING)
    private DayOfWeek day;

    protected RecurringTask() {
    }

    public RecurringTask(String name, String description, AppUser owner, DayOfWeek day) {
        super(name, description, owner);
        this.day = day;
    }

    public DayOfWeek getDay() {
        return day;
    }

    

    public void updateDetails(String name, String description, DayOfWeek day) {
        super.updateDetails(name, description);
        this.day = day;
    }
    
    @Override
    public void resetWeeklyStatus() {
        super.setCompleted(false);
        super.setCompletedAt(null);
        super.setCompletedByUsername(null);
    }


}
