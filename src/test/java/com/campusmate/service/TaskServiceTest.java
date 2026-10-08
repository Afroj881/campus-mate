package com.campusmate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusmate.model.Task;
import com.campusmate.model.User;
import com.campusmate.repository.TaskRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createTaskAlwaysAssignsCurrentOwnerAndPendingStatus() {
        User owner = new User();
        User submittedOwner = new User();
        Task task = new Task();
        task.setUser(submittedOwner);
        task.setStatus(Task.Status.COMPLETED);

        when(taskRepository.save(task)).thenReturn(task);

        Task createdTask = taskService.createTask(task, owner);

        assertSame(owner, createdTask.getUser());
        assertEquals(Task.Status.PENDING, createdTask.getStatus());
        verify(taskRepository).save(task);
    }

    @Test
    void lookupRejectsTaskThatDoesNotBelongToCurrentOwner() {
        User owner = new User();
        when(taskRepository.findByIdAndUser(42L, owner)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> taskService.getTaskForUser(42L, owner));
        verify(taskRepository).findByIdAndUser(42L, owner);
    }

    @Test
    void updateChangesTaskFieldsWithoutChangingOwnerOrStatus() {
        User owner = new User();
        User submittedOwner = new User();
        Task existingTask = new Task();
        existingTask.setUser(owner);
        existingTask.setStatus(Task.Status.COMPLETED);
        existingTask.setTitle("Original title");
        Task submittedTask = new Task();
        submittedTask.setUser(submittedOwner);
        submittedTask.setStatus(Task.Status.PENDING);
        submittedTask.setTitle("Updated title");
        submittedTask.setType("Exam");

        when(taskRepository.findByIdAndUser(42L, owner)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(existingTask)).thenReturn(existingTask);

        Task updatedTask = taskService.updateTask(42L, submittedTask, owner);

        assertSame(owner, updatedTask.getUser());
        assertEquals(Task.Status.COMPLETED, updatedTask.getStatus());
        assertEquals("Updated title", updatedTask.getTitle());
        assertEquals("Exam", updatedTask.getType());
    }

    @Test
    void markCompletedRequiresOwnershipAndChangesStatus() {
        User owner = new User();
        Task task = new Task();
        task.setStatus(Task.Status.PENDING);
        when(taskRepository.findByIdAndUser(42L, owner)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        Task completedTask = taskService.markTaskCompleted(42L, owner);

        assertEquals(Task.Status.COMPLETED, completedTask.getStatus());
        verify(taskRepository).findByIdAndUser(42L, owner);
    }
}
