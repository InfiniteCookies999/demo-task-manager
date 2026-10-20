package infinitecookies.task.manager.services;

import infinitecookies.task.manager.models.Category;
import infinitecookies.task.manager.models.Task;
import infinitecookies.task.manager.models.dto.CreateTaskRequest;
import infinitecookies.task.manager.models.dto.TaskResponse;
import infinitecookies.task.manager.models.dto.TaskUpdateRequest;
import infinitecookies.task.manager.repositories.BoardRepository;
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
    private final BoardService boardService;

    public TaskService(TaskRepository taskRepository,
                       CategoryRepository categoryRepository,
                       BoardService boardService) {
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
        this.boardService = boardService;
    }

    public TaskResponse createTask(Long userId, Long boardId, CreateTaskRequest request) {
        boardService.checkHasAuthorization(userId, boardId);

        Category category = categoryRepository.findByIdAndBoardId(request.categoryId(), boardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        // This is fine the user would have to create/delete over 2 billion times.
        int nextPosition = (int) taskRepository.countByCategoryId(request.categoryId());

        Task task = new Task();
        task.setName(request.name());
        task.setDescription(null);
        task.setCompleted(false);
        task.setPosition(nextPosition);
        task.setCategory(category);
        taskRepository.save(task);

        return TaskResponse.fromTask(task);
    }

    public List<TaskResponse> getTasks(Long userId, Long boardId) {
        boardService.checkHasAuthorization(userId, boardId);

        return taskRepository.findAllByBoardId(boardId)
            .stream().map(TaskResponse::fromTask).toList();
    }

    @Transactional
    public void updateTask(Long userId, Long boardId, Long id, TaskUpdateRequest request) {
        boardService.checkHasAuthorization(userId, boardId);

        Task task = taskRepository.findByIdAndCategoryBoardId(id, boardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        if (request.name() != null) {
            task.setName(request.name());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.completed() != null) {
            task.setCompleted(request.completed());
        }
    }

    @Transactional
    public void deleteTask(Long userId, Long boardId, Long id) {
        boardService.checkHasAuthorization(userId, boardId);

        Task task = taskRepository.findByIdAndCategoryBoardId(id, boardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        long categoryId = task.getCategory().getId();

        taskRepository.delete(task);
        taskRepository.flush();

        // Re-normalizing the positions.
        List<Task> tasks = taskRepository.findAllByCategoryIdOrderByPositionAsc(categoryId);
        for (int i= 0; i < tasks.size(); i++) {
            tasks.get(i).setPosition(i);
        }
    }
}
