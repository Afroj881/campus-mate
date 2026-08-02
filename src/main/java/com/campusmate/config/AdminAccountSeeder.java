package com.campusmate.config;
import com.campusmate.model.User;
import com.campusmate.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
@Component
public class AdminAccountSeeder implements ApplicationRunner {
 @Value("${campusmate.admin.email}") private String adminEmail;
 @Value("${campusmate.admin.password}") private String adminPassword;
 private final UserRepository users;
 private final PasswordEncoder encoder;
 public AdminAccountSeeder(UserRepository users,PasswordEncoder encoder){this.users=users;this.encoder=encoder;}
 @Override public void run(ApplicationArguments args){
  User admin=users.findByEmailIgnoreCase(adminEmail).orElseGet(User::new);
  admin.setName("Campus Mate Administrator"); admin.setEmail(adminEmail);
  admin.setPassword(encoder.encode(adminPassword)); admin.setRole(User.Role.ADMIN);
  admin.setDepartment("Administration"); admin.setSemester(1);
  users.save(admin);
 }
}
