package com.familyhome.task;

import com.familyhome.member.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tasks")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "task_type")
public abstract class BaseTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    @Column(nullable = false)
    private boolean completed = false;

    private Instant completedAt;
    private String completedByUsername;

    protected BaseTask() {}

    public BaseTask(String name, String description, AppUser owner) {
        this.name = name;
        this.description = description;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AppUser getOwner() {
        return owner;
    }

    public Long getAssigneeId() {
        return owner == null ? null : owner.getId();
    }

    public String getAssigneeUsername() {
        return owner == null ? null : owner.getUsername();
    }

    public abstract TaskType getType();

    public LocalDate getTaskDate() {
        return null;
    }

    public DayOfWeek getDay() {
        return null;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getCompletedByUsername() {
        return completedByUsername;
    }

    public void updateDetails(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public abstract void updateDetails(
            String name, String description, LocalDate taskDate, DayOfWeek day);

    public void assignOwner(AppUser owner) {
        this.owner = owner;
    }

    public void markAsCompleted(String completedByUsername, Instant completedAt) {
        if (this.completed) return;
        this.completed = true;
        this.completedAt = completedAt;
        this.completedByUsername = completedByUsername;
    }
}
