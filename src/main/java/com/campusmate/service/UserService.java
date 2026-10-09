package com.campusmate.service;

import com.campusmate.model.RegistrationForm;
import com.campusmate.model.ProfileForm;
import com.campusmate.model.User;
import com.campusmate.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerStudent(RegistrationForm form) {
        String email = normalizeEmail(form.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }

        User user = new User();
        user.setName(form.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(User.Role.STUDENT);
        user.setDepartment(form.getDepartment().trim());
        user.setSemester(form.getSemester());
        user.setSection(form.getSection().trim());

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }

    @Transactional
    public User createFaculty(String name, String email, String password, String department, Integer semester, String section) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException();
        }

        User faculty = new User();
        faculty.setName(name == null ? "Faculty" : name.trim());
        faculty.setEmail(normalizedEmail);
        faculty.setPassword(passwordEncoder.encode(password));
        faculty.setRole(User.Role.FACULTY);
        faculty.setDepartment(department == null ? "General" : department.trim());
        faculty.setSemester(semester == null ? 1 : semester);
        faculty.setSection(section == null || section.isBlank() ? "A" : section);
        return userRepository.save(faculty);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email));
    }

    @Transactional(readOnly = true)
    public long countStudents() {
        return userRepository.countByRole(User.Role.STUDENT);
    }

    @Transactional(readOnly = true)
    public long countFaculty() {
        return userRepository.countByRole(User.Role.FACULTY);
    }

    @Transactional(readOnly = true)
    public List<User> findFacultyUsers() {
        return userRepository.findAllByRole(User.Role.FACULTY);
    }

    @Transactional
    public User updateStudentProfile(String email, ProfileForm profileForm) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (user.getRole() != User.Role.STUDENT) {
            throw new org.springframework.security.access.AccessDeniedException("Only students can update a profile.");
        }

        user.setName(profileForm.getName().trim());
        user.setDepartment(profileForm.getDepartment().trim());
        user.setSemester(profileForm.getSemester());
        user.setSection(profileForm.getSection().trim());
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = normalizeEmail(username);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static class DuplicateEmailException extends RuntimeException {
    }
}
