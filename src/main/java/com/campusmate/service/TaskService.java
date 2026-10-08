package com.campusmate.service;

import com.campusmate.model.Task;
import com.campusmate.model.User;
import com.campusmate.repository.TaskRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Task createTask(Task task, User owner) {
        task.setId(null);
        task.setStatus(Task.Status.PENDING);
        task.setUser(owner);
        return taskRepository.save(task);
    }

    public List<Task> getTasksForUser(User user) {
        return taskRepository.findAllByUserOrderByDeadlineAscIdAsc(user);
    }

    public List<Task> getPendingTasksForUser(User user) {
        return taskRepository.findAllByUserAndStatusOrderByDeadlineAscIdAsc(user, Task.Status.PENDING);
    }

    public List<Task> getUpcomingPendingTasksForUser(User user) {
        return taskRepository.findTop5ByUserAndStatusOrderByDeadlineAscIdAsc(user, Task.Status.PENDING);
    }

    public long countPendingTasksForUser(User user) {
        return taskRepository.countByUserAndStatus(user, Task.Status.PENDING);
    }

    public Task getTaskForUser(Long id, User user) {
        return taskRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    public Task updateTask(Long id, Task updatedTask, User user) {
        Task existingTask = getTaskForUser(id, user);
        existingTask.setTitle(updatedTask.getTitle());
        existingTask.setDescription(updatedTask.getDescription());
        existingTask.setType(updatedTask.getType());
        existingTask.setDeadline(updatedTask.getDeadline());
        return taskRepository.save(existingTask);
    }

    public void deleteTask(Long id, User user) {
        taskRepository.delete(getTaskForUser(id, user));
    }

    public Task markTaskCompleted(Long id, User user) {
        Task task = getTaskForUser(id, user);
        task.setStatus(Task.Status.COMPLETED);
        return taskRepository.save(task);
    }
}
