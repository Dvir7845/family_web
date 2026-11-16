package com.familyhome.task;

import com.familyhome.member.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.InheritanceType;

@Entity
@Table(name = "tasks")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "task_type")
public abstract class BaseTask {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private AppUser owner;

    @Column(nullable = false)
    private boolean completed = false;

    private java.time.Instant completedAt;
    private String completedByUsername;

    protected BaseTask() {
    }

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

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public java.time.Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(java.time.Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getCompletedByUsername() {
        return completedByUsername;
    }

    public void setCompletedByUsername(String completedByUsername) {
        this.completedByUsername = completedByUsername;
    }

    public void updateDetails(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void assignOwner(AppUser owner) {
        this.owner = owner;
    }

    public void markAsCompleted(String completedByUsername) {
        if (this.completed) return; 
        this.completed = true;
        this.completedAt = java.time.Instant.now();
        this.completedByUsername = completedByUsername;
    }

    public boolean shouldDeleteOnDailyCleanup() { return false; }
    public void resetWeeklyStatus() {}
    

}