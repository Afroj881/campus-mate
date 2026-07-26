package com.campusmate.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
@Entity @Table(name="users")
public class User {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @NotBlank @Size(max=120) @Column(nullable=false,length=120) private String name;
 @NotBlank @Email @Size(max=190) @Column(nullable=false,unique=true,length=190) private String email;
 @NotBlank @Column(nullable=false,length=255) private String password;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Role role;
 @NotBlank @Size(max=120) @Column(nullable=false,length=120) private String department;
 @NotNull @Min(1) @Column(nullable=false) private Integer semester;
 public User() {}
 public Long getId(){return id;} public void setId(Long v){id=v;}
 public String getName(){return name;} public void setName(String v){name=v;}
 public String getEmail(){return email;} public void setEmail(String v){email=v;}
 public String getPassword(){return password;} public void setPassword(String v){password=v;}
 public Role getRole(){return role;} public void setRole(Role v){role=v;}
 public String getDepartment(){return department;} public void setDepartment(String v){department=v;}
 public Integer getSemester(){return semester;} public void setSemester(Integer v){semester=v;}
 public enum Role { STUDENT }
}
