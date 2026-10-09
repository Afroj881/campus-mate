package com.campusmate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "timetable_entries")
public class Timetable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Day is required.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private DayOfWeek day;

    @NotBlank(message = "Subject is required.")
    @Size(max = 255, message = "Subject must be 255 characters or fewer.")
    @Column(nullable = false)
    private String subject;

    @NotBlank(message = "Faculty is required.")
    @Size(max = 255, message = "Faculty must be 255 characters or fewer.")
    @Column(nullable = false)
    private String faculty;

    @NotBlank(message = "Room is required.")
    @Size(max = 255, message = "Room must be 255 characters or fewer.")
    @Column(nullable = false)
    private String room;

    @NotNull(message = "Start time is required.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @Column(nullable = false)
    private LocalTime startTime;

    @NotNull(message = "End time is required.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @Column(nullable = false)
    private LocalTime endTime;

    public Timetable() {
    }

    @AssertTrue(message = "End time must be later than start time.")
    public boolean isEndTimeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DayOfWeek getDay() {
        return day;
    }

    public void setDay(DayOfWeek day) {
        this.day = day;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}
