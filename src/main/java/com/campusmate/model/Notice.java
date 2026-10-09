package com.campusmate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Entity
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required.")
    @Size(max = 255, message = "Title must be 255 characters or fewer.")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Description is required.")
    @Size(max = 65535, message = "Description is too long.")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "Category is required.")
    @Size(max = 255, message = "Category must be 255 characters or fewer.")
    @Column(nullable = false)
    private String category;

    @NotNull(message = "Date is required.")
    @Column(nullable = false)
    private LocalDate date;

    public Notice() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}