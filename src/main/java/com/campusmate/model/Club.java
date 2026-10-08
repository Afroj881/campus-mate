package com.campusmate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "clubs")
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Club name is required.")
    @Size(max = 160, message = "Club name must be 160 characters or fewer.")
    @Column(nullable = false, length = 160)
    private String name;

    @NotBlank(message = "Description is required.")
    @Size(max = 65535, message = "Description is too long.")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "Faculty coordinator is required.")
    @Size(max = 160, message = "Faculty coordinator must be 160 characters or fewer.")
    @Column(nullable = false, length = 160)
    private String facultyCoordinator;

    @NotBlank(message = "Student coordinator is required.")
    @Size(max = 160, message = "Student coordinator must be 160 characters or fewer.")
    @Column(nullable = false, length = 160)
    private String studentCoordinator;

    @NotBlank(message = "Contact is required.")
    @Size(max = 190, message = "Contact must be 190 characters or fewer.")
    @Column(nullable = false, length = 190)
    private String contact;

    public Club() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFacultyCoordinator() {
        return facultyCoordinator;
    }

    public void setFacultyCoordinator(String facultyCoordinator) {
        this.facultyCoordinator = facultyCoordinator;
    }

    public String getStudentCoordinator() {
        return studentCoordinator;
    }

    public void setStudentCoordinator(String studentCoordinator) {
        this.studentCoordinator = studentCoordinator;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }
}