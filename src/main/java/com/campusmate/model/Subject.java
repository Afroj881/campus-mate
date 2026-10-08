package com.campusmate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "subjects", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subject_code", columnNames = "code")
})
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Subject name is required.")
    @Size(max = 180, message = "Subject name must be 180 characters or fewer.")
    @Column(nullable = false, length = 180)
    private String name;

    @NotBlank(message = "Subject code is required.")
    @Size(max = 50, message = "Subject code must be 50 characters or fewer.")
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @NotBlank(message = "Department is required.")
    @Size(max = 120, message = "Department must be 120 characters or fewer.")
    @Column(nullable = false, length = 120)
    private String department;

    @NotNull(message = "Semester is required.")
    @Min(value = 1, message = "Semester must be at least 1.")
    @Column(nullable = false)
    private Integer semester;

    public Subject() {
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }
}
