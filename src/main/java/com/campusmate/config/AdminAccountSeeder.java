package com.campusmate.config;

import com.campusmate.model.User;
import com.campusmate.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "campusmate.admin.seed.enabled", havingValue = "true", matchIfMissing = true)
public class AdminAccountSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminAccountSeeder.class);
    @Value("${campusmate.admin.email}")
    private String adminEmail;
    @Value("${campusmate.admin.password}")
    private String adminPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        User existingUser = userRepository.findByEmailIgnoreCase(adminEmail).orElse(null);

        if (existingUser == null) {
            User admin = new User();
            admin.setName("Campus Mate Administrator");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(User.Role.ADMIN);
            admin.setDepartment("Administration");
            admin.setSemester(1);

            userRepository.save(admin);
            logger.info("Created default admin account for {}.", adminEmail);
            return;
        }

        boolean changed = false;

        if (existingUser.getRole() != User.Role.ADMIN) {
            existingUser.setRole(User.Role.ADMIN);
            changed = true;
            logger.warn("Updated existing account {} to ADMIN role for admin access.", adminEmail);
        }

        if (!passwordEncoder.matches(adminPassword, existingUser.getPassword())) {
            existingUser.setPassword(passwordEncoder.encode(adminPassword));
            changed = true;
            logger.info("Updated BCrypt password for admin account {}.", adminEmail);
        }

        if (existingUser.getName() == null || existingUser.getName().isBlank()) {
            existingUser.setName("Campus Mate Administrator");
            changed = true;
        }

        if (existingUser.getDepartment() == null || existingUser.getDepartment().isBlank()) {
            existingUser.setDepartment("Administration");
            changed = true;
        }

        if (existingUser.getSemester() == null || existingUser.getSemester() < 1) {
            existingUser.setSemester(1);
            changed = true;
        }

        if (changed) {
            userRepository.save(existingUser);
        }
    }
}
