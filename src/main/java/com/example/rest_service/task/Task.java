package com.example.rest_service.task;

import com.example.rest_service.member.AppUser;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Column;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String day;
    private String description;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private AppUser owner;

    @Column(nullable = false)
    private boolean completed = false;

    protected Task() {
    }

    public Task(String name, String day, String description) {
        this.name = name;
        this.day = day;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDay() {
        return day;
    }

    public String getDescription() {
        return description;
    }

    public void updateDetails(
            String name,
            String day,
            String description) {
        this.name = name;
        this.day = day;
        this.description = description;
    }

    public void assignOwner(AppUser owner) {
        this.owner = owner;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markCompleted() {
        this.completed = true;
    }

    public String getAssigneeUsername() {
        return owner == null ? null : owner.getUsername();
    }
}