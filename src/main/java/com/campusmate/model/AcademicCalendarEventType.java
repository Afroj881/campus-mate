package com.campusmate.model;

public enum AcademicCalendarEventType {
    SEMESTER_START("Semester Start"),
    SEMESTER_END("Semester End"),
    EXAM("Examination"),
    HOLIDAY("Holiday"),
    COLLEGE_EVENT("College Event"),
    IMPORTANT_DATE("Important Academic Date");

    private final String displayName;

    AcademicCalendarEventType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
