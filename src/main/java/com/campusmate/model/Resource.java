package com.campusmate.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Column;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required.")
    @Size(max = 160, message = "Title must be 160 characters or fewer.")
    @Column(nullable = false, length = 160)
    private String title;

    @NotBlank(message = "Subject is required.")
    @Size(max = 160, message = "Subject must be 160 characters or fewer.")
    @Column(nullable = false, length = 160)
    private String subject;

    @NotBlank(message = "Description is required.")
    @Size(max = 65535, message = "Description is too long.")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Size(max = 2048, message = "URL must be 2048 characters or fewer.")
    @Pattern(regexp = "(?i)^(?:https?://\\S+)?\\z", message = "Enter a valid HTTP or HTTPS URL.")
    @Column(nullable = false, length = 2048)
    private String url;

    @Column(length = 255)
    private String originalFilename;
    @Column(length = 120)
    private String storedFilename;
    @Column(length = 120)
    private String contentType;
    private Long fileSize;
    @Column(length = 120)
    private String department;
    private Integer semester;
    @Column(length = 10)
    private String section;
    private LocalDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    public boolean isFileBacked() { return storedFilename != null && !storedFilename.isBlank(); }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String value) { originalFilename = value; }
    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String value) { storedFilename = value; }
    public String getContentType() { return contentType; }
    public void setContentType(String value) { contentType = value; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long value) { fileSize = value; }
    public String getDepartment() { return department; }
    public void setDepartment(String value) { department = value; }
    public Integer getSemester() { return semester; }
    public void setSemester(Integer value) { semester = value; }
    public String getSection() { return section; }
    public void setSection(String value) { section = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User value) { uploadedBy = value; }

    public Resource() {
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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
