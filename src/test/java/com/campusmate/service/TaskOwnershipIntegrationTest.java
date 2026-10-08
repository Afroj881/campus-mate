package com.campusmate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.campusmate.model.Task;
import com.campusmate.model.User;
import com.campusmate.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class TaskOwnershipIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void tasksAndMutationsAreScopedToTheirOwner() {
        User firstStudent = userRepository.save(createStudent("first"));
        User secondStudent = userRepository.save(createStudent("second"));
        Task firstTask = taskService.createTask(createTask("First student's task"), firstStudent);
        Task secondTask = taskService.createTask(createTask("Second student's task"), secondStudent);

        assertEquals(List.of(firstTask.getId()), taskService.getTasksForUser(firstStudent).stream().map(Task::getId).toList());
        assertEquals(List.of(secondTask.getId()), taskService.getTasksForUser(secondStudent).stream().map(Task::getId).toList());

        Task attemptedUpdate = createTask("Unauthorized update");
        attemptedUpdate.setStatus(Task.Status.COMPLETED);
        assertThrows(ResponseStatusException.class,
                () -> taskService.updateTask(firstTask.getId(), attemptedUpdate, secondStudent));
        assertThrows(ResponseStatusException.class,
                () -> taskService.deleteTask(firstTask.getId(), secondStudent));
        assertThrows(ResponseStatusException.class,
                () -> taskService.markTaskCompleted(firstTask.getId(), secondStudent));

        Task updatedTask = taskService.updateTask(firstTask.getId(), createTask("Updated first task"), firstStudent);
        assertEquals("Updated first task", updatedTask.getTitle());
        assertEquals(firstStudent.getId(), updatedTask.getUser().getId());
        assertEquals(Task.Status.PENDING, updatedTask.getStatus());

        taskService.markTaskCompleted(firstTask.getId(), firstStudent);
        assertEquals(0, taskService.countPendingTasksForUser(firstStudent));
        assertEquals(1, taskService.countPendingTasksForUser(secondStudent));

        taskService.deleteTask(firstTask.getId(), firstStudent);
        assertEquals(0, taskService.getTasksForUser(firstStudent).size());
        assertEquals(List.of(secondTask.getId()), taskService.getTasksForUser(secondStudent).stream().map(Task::getId).toList());
    }

    private User createStudent(String label) {
        User user = new User();
        user.setName("Planner Test " + label);
        user.setEmail("planner-" + label + "-" + UUID.randomUUID() + "@example.test");
        user.setPassword("not-used-by-test");
        user.setRole(User.Role.STUDENT);
        user.setDepartment("Testing");
        user.setSemester(1);
        return user;
    }

    private Task createTask(String title) {
        Task task = new Task();
        task.setTitle(title);
        task.setType("Assignment");
        task.setDeadline(LocalDate.now().plusDays(1));
        return task;
    }
}
