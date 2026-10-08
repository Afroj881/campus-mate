package com.campusmate.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RegistrationForm {

    @NotBlank(message = "Enter your name.")
    @Size(max = 120, message = "Name must be 120 characters or fewer.")
    private String name;

    @NotBlank(message = "Enter your email address.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 190, message = "Email must be 190 characters or fewer.")
    private String email;

    @NotBlank(message = "Create a password.")
    @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters.")
    private String password;

    @NotBlank(message = "Enter your department.")
    @Size(max = 120, message = "Department must be 120 characters or fewer.")
    private String department;

    @NotNull(message = "Select your semester.")
    @Min(value = 1, message = "Semester must be at least 1.")
    private Integer semester;

    @NotBlank(message = "Enter your section.")
    @Size(max = 10, message = "Section must be 10 characters or fewer.")
    private String section;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
