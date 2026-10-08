package com.campusmate.model;

public class ProfileDetails {

    private final String name;
    private final String email;
    private final String department;
    private final Integer semester;
    private final String section;
    private final User.Role role;

    public ProfileDetails(User user) {
        this.name = user.getName();
        this.email = user.getEmail();
        this.department = user.getDepartment();
        this.semester = user.getSemester();
        this.section = user.getSection();
        this.role = user.getRole();
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getDepartment() {
        return department;
    }

    public Integer getSemester() {
        return semester;
    }

    public String getSection() {
        return section;
    }

    public User.Role getRole() {
        return role;
    }
}
