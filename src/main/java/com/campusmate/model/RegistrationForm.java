package com.campusmate.model;
import jakarta.validation.constraints.*;
public class RegistrationForm {
 @NotBlank(message="Enter your name.") @Size(max=120) private String name;
 @NotBlank(message="Enter your email address.") @Email(message="Enter a valid email address.") @Size(max=190) private String email;
 @NotBlank(message="Create a password.") @Size(min=8,max=72,message="Password must be 8 to 72 characters.") private String password;
 @NotBlank(message="Enter your department.") @Size(max=120) private String department;
 @NotNull(message="Select your semester.") @Min(value=1,message="Semester must be at least 1.") private Integer semester;
 public String getName(){return name;} public void setName(String v){name=v;}
 public String getEmail(){return email;} public void setEmail(String v){email=v;}
 public String getPassword(){return password;} public void setPassword(String v){password=v;}
 public String getDepartment(){return department;} public void setDepartment(String v){department=v;}
 public Integer getSemester(){return semester;} public void setSemester(Integer v){semester=v;}
}
