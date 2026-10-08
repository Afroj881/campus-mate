package com.campusmate.repository;

import com.campusmate.model.User;
import com.campusmate.model.User.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    long countByRole(Role role);

    List<User> findAllByRole(Role role);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCase(String email);
}
