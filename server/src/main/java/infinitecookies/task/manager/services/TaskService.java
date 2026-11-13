package infinitecookies.task.manager.services;

import infinitecookies.task.manager.exceptions.ApiErrorCode;
import infinitecookies.task.manager.exceptions.ApiException;
import infinitecookies.task.manager.models.entity.Category;
import infinitecookies.task.manager.models.entity.Task;
import infinitecookies.task.manager.models.entity.User;
import infinitecookies.task.manager.models.dto.*;
import infinitecookies.task.manager.models.websock.*;
import infinitecookies.task.manager.repositories.*;
import infinitecookies.task.manager.utils.UserSearchParser;
import infinitecookies.task.manager.utils.UserSearchParts;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final BoardService boardService;
    private final BoardRevisionService boardRevisionService;

    private final ApplicationEventPublisher applicationEventPublisher;

    public TaskService(TaskRepository taskRepository,
                       CategoryRepository categoryRepository,
                       BoardRepository boardRepository,
                       UserRepository userRepository,
                       BoardService boardService,
                       BoardRevisionService boardRevisionService,
                       ApplicationEventPublisher applicationEventPublisher) {
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
        this.boardService = boardService;
        this.boardRevisionService = boardRevisionService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public TaskResponse createTask(Long userId, Long boardId, CreateTaskRequest request) {
        boardService.checkHasAuthorization(userId, boardId);

        Category category = categoryRepository.findByIdAndBoardId(request.categoryId(), boardId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, ApiErrorCode.CATEGORY_NOT_FOUND, "Category not found"
            ));

        int nextPosition = (int) taskRepository.countByCategoryId(request.categoryId());

        if (nextPosition >= Category.MAX_TASKS) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                ApiErrorCode.TASK_LIMIT_EXCEEDED,
                "A category cannot have more than " + Category.MAX_TASKS + " tasks"
            );
        }

        Task task = new Task();
        task.setName(request.name());
        task.setPosition(nextPosition);
        task.setCategory(category);
        task.setCreatedByUser(userRepository.getReferenceById(userId));
        taskRepository.save(task);

        TaskResponse response = TaskResponse.fromTask(task, false);

        long revision = boardRevisionService.incrementRevision(boardId);
        applicationEventPublisher.publishEvent(new TaskCreatedEvent(boardId, revision, response));

        return response;
    }

    public TaskListResponse getTasks(Long userId, Long boardId) {
        boardService.checkHasAuthorization(userId, boardId);

        long revision = boardRevisionService.getRevision(boardId);
        List<TaskResponse> tasks = taskRepository.findTasksByBoardId(userId, boardId).stream()
            .map(projection -> new TaskResponse(
                projection.getId(),
                projection.getCategoryId(),
                projection.getName(),
                projection.getDescription(),
                projection.getCompleted(),
                projection.getPosition(),
                projection.getCreatedByUserId(),
                projection.getCreatedByUsername(),
                projection.getAssignedToYou()
            ))
            .toList();
        return new TaskListResponse(revision, tasks);
    }

    public List<TaskAssigneeResponse> getTaskAssignees(Long userId, Long boardId, Long id) {
        boardService.checkHasAuthorization(userId, boardId);

        return taskRepository.findAllAssigneesByIdAndBoardIdAndBoardId(id, boardId)
            .stream()
            .map(user -> new TaskAssigneeResponse(user.getId(), user.getUsername()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskAssigneeSearchResponse> searchAssignees(Long sessionUserId, Long boardId, Long id,
                                                            String search) {
        boardService.checkHasAuthorization(sessionUserId, boardId);

        Task task = taskRepository.findByIdAndCategoryBoardId(id, boardId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, ApiErrorCode.TASK_NOT_FOUND, "Task not found"
            ));

        Set<Long> assignedUsers = task.getAssignees().stream()
            .map(User::getId)
            .collect(Collectors.toSet());

        UserSearchParts searchParts = UserSearchParser.parse(search);

        return userRepository.searchAssignees(
                searchParts.username(),
                searchParts.userId(),
                boardId,
                PageRequest.of(0, 10)
            )
            .stream()
            .map(user -> new TaskAssigneeSearchResponse(
                user.getId(),
                user.getUsername(),
                assignedUsers.contains(user.getId())
            ))
            .toList();
    }

    @Transactional
    public void updateTask(Long userId, Long boardId, Long id, TaskUpdateRequest request) {
        boardService.checkHasAuthorization(userId, boardId);

        Task task = taskRepository.findByIdAndCategoryBoardId(id, boardId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, ApiErrorCode.TASK_NOT_FOUND, "Task not found"
            ));

        if (request.name() != null) {
            task.setName(request.name());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.completed() != null) {
            task.setCompleted(request.completed());
        }
        if (request.assignedUserIds() != null) {
            Set<Long> requestedIds = new HashSet<>(request.assignedUserIds());
            List<User> users = userRepository.findAllowedUsersByBoardIdAndUserIds(
                boardId,
                requestedIds
            );

            if (users.size() != requestedIds.size()) {
                throw new ApiException(
                    HttpStatus.BAD_REQUEST, ApiErrorCode.TASK_ASSIGNEE_NOT_MEMBER_OF_BOARD,
                    "One or more users are not members of this board"
                );
            }

            task.setAssignees(new HashSet<>(users));
        }

        long revision = boardRevisionService.incrementRevision(boardId);
        applicationEventPublisher.publishEvent(new TaskUpdatedEvent(
            boardId,
            revision,
            id,
            request.name(),
            request.description(),
            request.completed(),
            request.assignedUserIds()
        ));
    }

    @Transactional
    public void reorderTasks(Long userId, Long boardId, TaskReorderRequest request) {

        // Positional updates requires updating multiple tasks at the same time and this method
        // has run into a deadlock. Locking based on the board to prevent deadlocking.
        boardRepository.findByIdForReorderWithLock(boardId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, ApiErrorCode.BOARD_NOT_FOUND, "Board not found"
            ));

        boardService.checkHasAuthorization(userId, boardId);

        Category targetCategory =
            categoryRepository.findByIdAndBoardId(request.targetCategoryId(), boardId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, ApiErrorCode.TASK_TARGET_CATEGORY_NOT_FOUND,
                "Target category not found"
            ));

        List<Task> tasks = findAndValidateTasksForReorder(boardId, request);

        // Reorder the tasks to match the request order so the positions may be updated
        // correctly.
        Map<Long, Task> tasksById = tasks.stream()
            .collect(Collectors.toMap(Task::getId, Function.identity()));

        tasks = request.taskIds().stream()
            .map(tasksById::get)
            .toList();

        Task movedTask = getAndValidateMovedTaskForReorder(request, tasks, targetCategory);

        // Validate all the target category tasks were specified.
        long targetCategoryCount = taskRepository.countByCategoryId(request.targetCategoryId());
        long tasksInTargetCount = tasks.size();
        if (movedTask != null) {
            // The moved task is not currently in the target category.
            --tasksInTargetCount;
        }
        if (tasksInTargetCount != targetCategoryCount) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST, ApiErrorCode.TASK_REORDER_MISSING_TASK,
                "Must specify the re-ordering for all tasks"
            );
        }

        // Updating specific logic
        // ---------------------------

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

        List<Task> sourceTasks = null;
        if (movedTask != null) {
            // Re-normalizing the positions in the category the task was moved from.
            taskRepository.flush();

            sourceTasks = taskRepository.findAllByCategoryIdOrderByPositionAsc(sourceCategoryId);
            for (int i= 0; i < sourceTasks.size(); i++) {
                sourceTasks.get(i).setPosition(i);
            }
        }

        long revision = boardRevisionService.incrementRevision(boardId);
        applicationEventPublisher.publishEvent(new TasksReorderedEvent(
            boardId,
            revision,
            sourceCategoryId,
            request.targetCategoryId(),
            sourceTasks != null ? sourceTasks.stream().map(Task::getId).toList() : null,
            request.taskIds(),
            request.movedTaskId()
        ));
    }

    private List<Task> findAndValidateTasksForReorder(Long boardId, TaskReorderRequest request) {

        // Check for duplicates.
        Set<Long> uniqueIds = new HashSet<>(request.taskIds());
        if (uniqueIds.size() != request.taskIds().size()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST, ApiErrorCode.TASK_DUPLICATE_ID,
                "Duplicate task IDs are not allowed"
            );
        }

        // Find all tasks including the moved task.
        List<Task> tasks = taskRepository.findByIdInAndCategoryBoardId(request.taskIds(), boardId);
        if (tasks.size() != request.taskIds().size()) {
            throw new ApiException(
                HttpStatus.NOT_FOUND, ApiErrorCode.TASK_NOT_FOUND,
                "One or more tasks not found"
            );
        }

        if (tasks.size() > Category.MAX_TASKS) {
            throw new ApiException(
                HttpStatus.CONFLICT, ApiErrorCode.TASK_LIMIT_EXCEEDED,
                "A category cannot have more than " + Category.MAX_TASKS + " tasks"
            );
        }

        // Make sure all provided task IDs are either the moved task or already in target
        // category.
        for (Task task : tasks) {
            if (!Objects.equals(task.getCategory().getId(), request.targetCategoryId())) {
                if (!Objects.equals(task.getId(), request.movedTaskId())) {
                    throw new ApiException(
                        HttpStatus.BAD_REQUEST, ApiErrorCode.TASK_NEITHER_MOVED_OR_IN_TARGET_CATEGORY,
                        "Task neither the moved task or in target category"
                    );
                }
            }
        }

        return tasks;
    }

    private Task getAndValidateMovedTaskForReorder(
        TaskReorderRequest request, List<Task> tasks, Category targetCategory
    ) {
        if (request.movedTaskId() == null) {
            return null;
        }

        Task movedTask = tasks.stream()
            .filter(task -> Objects.equals(task.getId(), request.movedTaskId()))
            .findFirst()
            .orElseThrow(() ->
                new ApiException(
                    HttpStatus.BAD_REQUEST, ApiErrorCode.TASK_MOVED_TASK_SHOULD_BE_IN_TASK_IDS,
                    "Moved task should be in taskIds"
                ));

        Long sourceCategoryId = movedTask.getCategory().getId();
        if (Objects.equals(sourceCategoryId, targetCategory.getId())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST, ApiErrorCode.TASK_MOVED_TASK_MUST_BE_IN_DIFF_CATEGORY,
                "Moved task must belong to a different category");
        }
        return movedTask;
    }

    @Transactional
    public void deleteTask(Long userId, Long boardId, Long id) {
        boardService.checkHasAuthorization(userId, boardId);

        // Fetch the task since we need to know its category to renormalize the positions.
        Task task = taskRepository.findByIdAndCategoryBoardId(id, boardId)
            .orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.TASK_NOT_FOUND, "Task not found"
            ));

        long categoryId = task.getCategory().getId();

        taskRepository.delete(task);
        taskRepository.flush();

        // Re-normalizing the positions.
        List<Task> tasks = taskRepository.findAllByCategoryIdOrderByPositionAsc(categoryId);
        for (int i= 0; i < tasks.size(); i++) {
            tasks.get(i).setPosition(i);
        }

        long revision = boardRevisionService.incrementRevision(boardId);
        applicationEventPublisher.publishEvent(new TaskDeletedEvent(boardId, revision, categoryId, id));
    }
}
