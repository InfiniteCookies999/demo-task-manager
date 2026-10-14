package infinitecookies.task.manager.services;

import infinitecookies.task.manager.models.Category;
import infinitecookies.task.manager.models.Task;
import infinitecookies.task.manager.models.dto.CreateTaskRequest;
import infinitecookies.task.manager.models.dto.TaskResponse;
import infinitecookies.task.manager.models.dto.TaskUpdateRequest;
import infinitecookies.task.manager.repositories.CategoryRepository;
import infinitecookies.task.manager.repositories.TaskRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;

    public TaskService(TaskRepository taskRepository, CategoryRepository categoryRepository) {
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
    }

    public TaskResponse createTask(String sessionId, CreateTaskRequest request) {

        Category category = categoryRepository.findByIdAndSessionId(request.categoryId(), sessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        // This is fine the user would have to create/delete over 2 billion times.
        int nextPosition = (int) taskRepository.countBySessionIdAndCategoryId(sessionId, request.categoryId());

        Task task = new Task();
        task.setSessionId(sessionId);
        task.setName(request.name());
        task.setDescription(null);
        task.setCompleted(false);
        task.setPosition(nextPosition);
        task.setCategory(category);
        taskRepository.save(task);

        return TaskResponse.fromTask(task);
    }

    public List<TaskResponse> getTasksForCategory(String sessionId, Long categoryId) {
        return taskRepository.findAllBySessionIdAndCategoryIdOrderByPositionAsc(sessionId, categoryId)
            .stream().map(TaskResponse::fromTask).toList();
    }

    @Transactional
    public void updateTask(Long id, String sessionId, TaskUpdateRequest request) {
        Task task = taskRepository.findByIdAndSessionId(id, sessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        if (request.name() != null) {
            task.setName(request.name());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }

        taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long id, String sessionId) {
        Task task = taskRepository.findByIdAndSessionId(id, sessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        long categoryId = task.getCategory().getId();

        taskRepository.deleteByIdAndSessionId(id, sessionId);

        // Re-normalizing the positions.
        List<Task> tasks = taskRepository
            .findAllBySessionIdAndCategoryIdOrderByPositionAsc(sessionId, categoryId);
        for (int i= 0; i < tasks.size(); i++) {
            tasks.get(i).setPosition(i);
        }
    }
}
