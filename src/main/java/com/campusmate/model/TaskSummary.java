package com.campusmate.model;

import java.time.LocalDate;

public class TaskSummary {

    private final Long id;
    private final String title;
    private final String description;
    private final String type;
    private final LocalDate deadline;
    private final Task.Status status;

    public TaskSummary(Task task) {
        this.id = task.getId();
        this.title = task.getTitle();
        this.description = task.getDescription();
        this.type = task.getType();
        this.deadline = task.getDeadline();
        this.status = task.getStatus();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public Task.Status getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return status == Task.Status.COMPLETED ? "Completed" : "Pending";
    }
}