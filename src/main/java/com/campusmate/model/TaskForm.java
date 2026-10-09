package com.campusmate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class TaskForm {

    @NotBlank(message = "Title is required.")
    @Size(max = 255, message = "Title must be 255 characters or fewer.")
    private String title;

    @Size(max = 65535, message = "Description is too long.")
    private String description;

    @NotBlank(message = "Type is required.")
    @Pattern(regexp = "Assignment|Exam|Project|Study|Other", message = "Choose a valid task type.")
    private String type;

    @NotNull(message = "Deadline is required.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;

    public TaskForm() {
    }

    public TaskForm(Task task) {
        this.title = task.getTitle();
        this.description = task.getDescription();
        this.type = task.getType();
        this.deadline = task.getDeadline();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }
}