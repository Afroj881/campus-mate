package com.campusmate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "teacher_subject_assignments",
        uniqueConstraints = @UniqueConstraint(name = "uk_teacher_subject_class",
                columnNames = {"faculty_id", "subject_id", "semester", "section"}))
public class TeacherSubjectAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private User faculty;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @NotBlank(message = "Department is required.")
    @Size(max = 120, message = "Department must be 120 characters or fewer.")
    @Column(nullable = false, length = 120)
    private String department;

    @NotNull(message = "Semester is required.")
    @Min(value = 1, message = "Semester must be at least 1.")
    @Column(nullable = false)
    private Integer semester;

    @NotBlank(message = "Section is required.")
    @Size(max = 10, message = "Section must be 10 characters or fewer.")
    @Column(nullable = false, length = 10)
    private String section;

    public TeacherSubjectAssignment() {
    }

    public TeacherSubjectAssignment(User faculty, Subject subject, String department, Integer semester, String section) {
        this.faculty = faculty;
        this.subject = subject;
        this.department = department;
        this.semester = semester;
        this.section = section;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getFaculty() {
        return faculty;
    }

    public void setFaculty(User faculty) {
        this.faculty = faculty;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
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
        this.section = section;
    }
}
