package com.campusmate.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProfileForm {

    @NotBlank(message = "Name is required.")
    @Size(max = 120, message = "Name must be 120 characters or fewer.")
    private String name;

    @NotBlank(message = "Department is required.")
    @Size(max = 120, message = "Department must be 120 characters or fewer.")
    private String department;

    @NotNull(message = "Semester is required.")
    @Min(value = 1, message = "Semester must be at least 1.")
    private Integer semester;

    @NotBlank(message = "Section is required.")
    @Size(max = 10, message = "Section must be 10 characters or fewer.")
    private String section;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section == null ? null : section.trim();
    }
}
