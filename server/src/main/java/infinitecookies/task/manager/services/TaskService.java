package infinitecookies.task.manager.services;

import infinitecookies.task.manager.models.Category;
import infinitecookies.task.manager.models.Task;
import infinitecookies.task.manager.models.dto.CreateTaskRequest;
import infinitecookies.task.manager.models.dto.TaskReorderRequest;
import infinitecookies.task.manager.models.dto.TaskResponse;
import infinitecookies.task.manager.models.dto.TaskUpdateRequest;
import infinitecookies.task.manager.repositories.CategoryRepository;
import infinitecookies.task.manager.repositories.TaskRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public void reorderTasks(Long userId, Long boardId, TaskReorderRequest request) {
        boardService.checkHasAuthorization(userId, boardId);

        // TODO (maddie): its unclear if this logic catches duplicates. When we write tests we
        //                should explicitly check for that.

        Category targetCategory =
            categoryRepository.findByIdAndBoardId(request.targetCategoryId(), boardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target category not found"));

        // Find all tasks including the moved task.
        List<Task> tasks = taskRepository.findByIdInAndCategoryBoardId(request.taskIds(), boardId);
        if (tasks.size() != request.taskIds().size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more tasks not found");
        }

        // Reorder the tasks to match the request order so the positions may be updated
        // correctly.
        Map<Long, Task> tasksById = tasks.stream()
            .collect(Collectors.toMap(Task::getId, Function.identity()));

        tasks = request.taskIds().stream()
            .map(tasksById::get)
            .toList();

        // Make sure all provided task IDs are either the moved task or already in the target
        // category.
        validateTasksInTargetCategoryForReorder(request, tasks);

        Task movedTask = getAndValidateMovedTaskForReorder(request, tasks, targetCategory);

        // Validate all the target category tasks were specified.
        long targetCategoryCount = taskRepository.countByCategoryId(request.targetCategoryId());
        long tasksInTargetCount = tasks.size();
        if (movedTask != null) {
            // The moved task is not currently in the target category.
            --tasksInTargetCount;
        }
        if (tasksInTargetCount != targetCategoryCount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must specify the re-ordering for all tasks");
        }

        // Updating specific logic
        // ---------------------------

        // Store reference to the
        Long sourceCategoryId = null;
        if (movedTask != null) {
            // Store reference to the original source category so its positions may be
            // re-normalized later.
            sourceCategoryId = movedTask.getCategory().getId();
            // Change the category to which the task belongs.
            movedTask.setCategory(targetCategory);
        }

        // Update the tasks positions in the target category.
        for (int i = 0; i < tasks.size(); i++) {
            tasks.get(i).setPosition(i);
        }

        if (movedTask != null) {
            // Re-normalizing the positions in the category the task was moved from.
            taskRepository.flush();

            tasks = taskRepository.findAllByCategoryIdOrderByPositionAsc(sourceCategoryId);
            for (int i= 0; i < tasks.size(); i++) {
                tasks.get(i).setPosition(i);
            }
        }
    }

    private void validateTasksInTargetCategoryForReorder(TaskReorderRequest request, List<Task> tasks) {
        for (Task task : tasks) {
            if (!Objects.equals(task.getCategory().getId(), request.targetCategoryId())) {
                if (!Objects.equals(task.getId(), request.movedCardId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Task neither the moved or in target category");
                }
            }
        }
    }

    private Task getAndValidateMovedTaskForReorder(
        TaskReorderRequest request, List<Task> tasks, Category targetCategory
    ) {
        if (request.movedCardId() == null) {
            return null;
        }

        Task movedTask = tasks.stream()
            .filter(task -> Objects.equals(task.getId(), request.movedCardId()))
            .findFirst()
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Moved task should be in taskIds")
            );

        Long sourceCategoryId = movedTask.getCategory().getId();
        if (Objects.equals(sourceCategoryId, targetCategory.getId())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Moved task must belong to a different category"
            );
        }
        return movedTask;
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
