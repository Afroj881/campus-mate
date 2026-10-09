package com.campusmate.repository;

import com.campusmate.model.Task;
import com.campusmate.model.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByUserOrderByDeadlineAscIdAsc(User user);

    List<Task> findAllByUserAndStatusOrderByDeadlineAscIdAsc(User user, Task.Status status);

    List<Task> findTop5ByUserAndStatusAndDeadlineGreaterThanEqualOrderByDeadlineAscIdAsc(
            User user, Task.Status status, LocalDate deadline);

    Optional<Task> findByIdAndUser(Long id, User user);

    long countByUserAndStatus(User user, Task.Status status);
}
