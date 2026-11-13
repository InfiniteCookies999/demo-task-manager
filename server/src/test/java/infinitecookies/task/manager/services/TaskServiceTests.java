package infinitecookies.task.manager.services;

import infinitecookies.task.manager.exceptions.ApiErrorCode;
import infinitecookies.task.manager.exceptions.ApiException;
import infinitecookies.task.manager.models.dto.*;
import infinitecookies.task.manager.models.entity.Board;
import infinitecookies.task.manager.models.entity.Category;
import infinitecookies.task.manager.models.entity.Task;
import infinitecookies.task.manager.models.entity.User;
import infinitecookies.task.manager.models.websock.TaskCreatedEvent;
import infinitecookies.task.manager.models.websock.TaskDeletedEvent;
import infinitecookies.task.manager.models.websock.TaskUpdatedEvent;
import infinitecookies.task.manager.models.websock.TasksReorderedEvent;
import infinitecookies.task.manager.repositories.BoardRepository;
import infinitecookies.task.manager.repositories.CategoryRepository;
import infinitecookies.task.manager.repositories.TaskRepository;
import infinitecookies.task.manager.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTests {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private BoardRevisionService boardRevisionService;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        // Manually setup mocks to test authorization at the boardService level.
        BoardService boardService = new BoardService(
            boardRevisionService,
            boardRepository,
            applicationEventPublisher
        );

        taskService = new TaskService(
            taskRepository,
            categoryRepository,
            boardRepository,
            userRepository,
            boardService,
            boardRevisionService,
            applicationEventPublisher
        );
    }

    @Test
    void createTaskLacksBoardAuthorization() {

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(false);

        CreateTaskRequest request = mock(CreateTaskRequest.class);
        ApiException e = assertThrows(ApiException.class, () -> taskService.createTask(1L, 1L, request));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        verify(taskRepository, never()).save(any());
        verify(boardRevisionService, never()).incrementRevision(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void createTaskForTooManyTasks() {
        Category category = new Category();

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(taskRepository.countByCategoryId(1L)).thenReturn((long) Category.MAX_TASKS);
        when(categoryRepository.findByIdAndBoardId(1L, 1L)).thenReturn(Optional.of(category));

        CreateTaskRequest request = new CreateTaskRequest(1L, "New Task");
        ApiException e = assertThrows(ApiException.class, () -> taskService.createTask(1L, 1L, request));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(taskRepository, never()).save(any());
        verify(boardRevisionService, never()).incrementRevision(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void createTaskSuccess() {

        Long userId = 1L;
        Long categoryId = 4L;

        User user = new User();
        user.setId(userId);

        Category category = new Category();
        category.setId(categoryId);

        when(boardRepository.isUserAllowed(1L, userId)).thenReturn(true);
        when(taskRepository.countByCategoryId(categoryId)).thenReturn(3L);
        when(categoryRepository.findByIdAndBoardId(categoryId, 1L)).thenReturn(Optional.of(category));
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        CreateTaskRequest request = new CreateTaskRequest(categoryId, "New Task");
        TaskResponse response = taskService.createTask(userId, 1L, request);

        assertNotNull(response);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());

        Task savedTask = taskCaptor.getValue();
        assertEquals("New Task", savedTask.getName());
        assertEquals(3, savedTask.getPosition());
        assertEquals(categoryId, savedTask.getCategory().getId());
        assertEquals(user.getId(), savedTask.getCreatedByUser().getId());

        verify(applicationEventPublisher).publishEvent(any(TaskCreatedEvent.class));
    }

    @Test
    void getTasksLacksBoardAuthorization() {

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(false);

        ApiException e = assertThrows(ApiException.class, () -> taskService.getTasks(1L, 1L));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
    }

    @Test
    void updateTaskLacksBoardAuthorization() {

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(false);

        TaskUpdateRequest request = mock(TaskUpdateRequest.class);
        ApiException e = assertThrows(ApiException.class, () -> taskService.updateTask(1L, 1L, 1L, request));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        verify(taskRepository, never()).findByIdAndCategoryBoardId(any(), any());
        verify(boardRevisionService, never()).incrementRevision(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateTaskAssigneeIdsNotMemberOfBoard() {

        Task task = new Task();

        List<Long> assignedUserIds = List.of(4L, 2L, 8L);
        Set<Long> assignedUserIdSet = new HashSet<>(assignedUserIds);

        User user1 = new User();
        user1.setId(4L);

        User user3 = new User();
        user3.setId(8L);

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(taskRepository.findByIdAndCategoryBoardId(1L, 1L)).thenReturn(Optional.of(task));
        when(userRepository.findAllowedUsersByBoardIdAndUserIds(1L, assignedUserIdSet))
            .thenReturn(List.of(user1, user3));

        TaskUpdateRequest request = new TaskUpdateRequest(
            null, null, null, assignedUserIds
        );
        ApiException e = assertThrows(ApiException.class, () -> taskService.updateTask(1L, 1L, 1L, request));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(boardRevisionService, never()).incrementRevision(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateTaskSuccessful() {

        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode description = objectMapper.readTree(
            """
                {
                    "root": {
                        "children": [
                            {
                                "children": [
                                    {
                                        "detail": 0,
                                        "format": 0,
                                        "mode": "normal",
                                        "style": "",
                                        "text": "Example test",
                                        "type": "text",
                                        "version": 1
                                    }
                                ],
                                "direction": null,
                                "format": "",
                                "indent": 0,
                                "textFormat": 0,
                                "textStyle": "",
                                "type": "paragraph",
                                "version": 1
                            }
                        ],
                        "direction": null,
                        "format": "",
                        "indent": 0,
                        "type": "root",
                        "version": 1
                    }
                }
            """
        );

        Task task = new Task();

        List<Long> assignedUserIds = List.of(4L, 2L, 8L);
        Set<Long> assignedUserIdSet = new HashSet<>(assignedUserIds);

        User user1 = new User();
        user1.setId(4L);

        User user2 = new User();
        user2.setId(2L);

        User user3 = new User();
        user3.setId(8L);

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(taskRepository.findByIdAndCategoryBoardId(1L, 1L)).thenReturn(Optional.of(task));
        when(userRepository.findAllowedUsersByBoardIdAndUserIds(1L, assignedUserIdSet))
            .thenReturn(List.of(user1, user2, user3));

        TaskUpdateRequest request = new TaskUpdateRequest(
            "New name",
            description,
            true,
            assignedUserIds
        );
        taskService.updateTask(1L, 1L, 1L, request);

        assertEquals("New name", task.getName());
        assertEquals(description, task.getDescription());
        assertTrue(task.isCompleted());
        assertEquals(
            assignedUserIdSet,
            task.getAssignees().stream()
                .map(User::getId)
                .collect(Collectors.toSet())
        );

        verify(applicationEventPublisher).publishEvent(any(TaskUpdatedEvent.class));
    }

    @Test
    void reorderTasksLacksBoardAuthorization() {

        Board board = new Board();

        when(boardRepository.findByIdForReorderWithLock(1L)).thenReturn(Optional.of(board));
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(false);

        TaskReorderRequest request = mock(TaskReorderRequest.class);
        ApiException e = assertThrows(ApiException.class, () -> taskService.reorderTasks(1L, 1L, request));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        verify(taskRepository, never()).findByIdInAndCategoryBoardId(any(), any());
        verify(boardRevisionService, never()).incrementRevision(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void reorderTasksTargetCategoryNotFound() {

        long targetCategoryId = 1L;

        Board board = new Board();

        when(boardRepository.findByIdForReorderWithLock(1L)).thenReturn(Optional.of(board));
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.findByIdAndBoardId(targetCategoryId, 1L)).thenReturn(Optional.empty());

        TaskReorderRequest request = new TaskReorderRequest(targetCategoryId, List.of(), null);
        ApiException e = assertThrows(ApiException.class, () -> taskService.reorderTasks(1L, 1L, request));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        assertEquals(ApiErrorCode.TASK_TARGET_CATEGORY_NOT_FOUND, e.getCode());

        verify(taskRepository, never()).findByIdInAndCategoryBoardId(any(), any());

        verify(taskRepository, never()).countByCategoryId(targetCategoryId);
        verify(boardRevisionService, never()).incrementRevision(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void reorderTasksTaskNotFound() {
        long targetCategoryId = 1L;

        Board board = new Board();

        Category targetCategory = new Category();
        targetCategory.setId(targetCategoryId);

        Task task = new Task();
        task.setId(1L);
        task.setCategory(targetCategory);

        when(boardRepository.findByIdForReorderWithLock(1L)).thenReturn(Optional.of(board));
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.findByIdAndBoardId(targetCategoryId, 1L))
            .thenReturn(Optional.of(targetCategory));

        when(taskRepository.findByIdInAndCategoryBoardId(List.of(1L, 2L), 1L))
            .thenReturn(List.of(task));

        TaskReorderRequest request =
            new TaskReorderRequest(targetCategoryId, List.of(1L, 2L), null);

        ApiException e = assertThrows(ApiException.class, () -> taskService.reorderTasks(1L, 1L, request));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        assertEquals(ApiErrorCode.TASK_NOT_FOUND, e.getCode());

        verify(taskRepository, never()).countByCategoryId(targetCategoryId);
        verify(boardRevisionService, never()).incrementRevision(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void reorderTasksCausesTooManyTasksInTargetCategory() {
        long targetCategoryId = 1L;

        Board board = new Board();

        Category targetCategory = new Category();
        targetCategory.setId(targetCategoryId);

        // Create enough tasks to exceed the maximum limit.
        List<Task> tasks = IntStream.range(0, Category.MAX_TASKS + 1)
            .mapToObj(i -> {
                Task task = new Task();
                task.setId((long) i);
                task.setCategory(targetCategory);
                return task;
            })
            .toList();

        List<Long> taskIds = tasks.stream()
            .map(Task::getId)
            .toList();

        when(boardRepository.findByIdForReorderWithLock(1L)).thenReturn(Optional.of(board));
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.findByIdAndBoardId(targetCategoryId, 1L)).thenReturn(Optional.of(targetCategory));

        when(taskRepository.findByIdInAndCategoryBoardId(taskIds, 1L)).thenReturn(tasks);

        TaskReorderRequest request =
            new TaskReorderRequest(targetCategoryId, taskIds, null);

        ApiException e = assertThrows(ApiException.class, () -> taskService.reorderTasks(1L, 1L, request));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());

        verify(taskRepository, never()).countByCategoryId(targetCategoryId);
        verify(boardRevisionService, never()).incrementRevision(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void reorderTasksContainsDuplicateIds() {
        long targetCategoryId = 1L;

        Board board = new Board();

        Category targetCategory = new Category();
        targetCategory.setId(targetCategoryId);

        when(boardRepository.findByIdForReorderWithLock(1L)).thenReturn(Optional.of(board));
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.findByIdAndBoardId(targetCategoryId, 1L))
            .thenReturn(Optional.of(targetCategory));

        TaskReorderRequest request =
            new TaskReorderRequest(targetCategoryId, List.of(1L, 2L, 2L), null);

        ApiException e = assertThrows(ApiException.class, () -> taskService.reorderTasks(1L, 1L, request));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals(ApiErrorCode.TASK_DUPLICATE_ID, e.getCode());

        verify(taskRepository, never()).countByCategoryId(targetCategoryId);
        verify(boardRevisionService, never()).incrementRevision(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void reorderTasksContainsTaskFromDifferentCategory() {
        long targetCategoryId = 1L;

        Board board = new Board();

        Category targetCategory = new Category();
        targetCategory.setId(targetCategoryId);

        Category otherCategory = new Category();
        otherCategory.setId(2L);

        Task task = new Task();
        task.setId(1L);
        task.setCategory(otherCategory);

        List<Long> taskIds = List.of(1L);

        when(boardRepository.findByIdForReorderWithLock(1L)).thenReturn(Optional.of(board));
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.findByIdAndBoardId(targetCategoryId, 1L)).thenReturn(Optional.of(targetCategory));

        when(taskRepository.findByIdInAndCategoryBoardId(taskIds, 1L))
            .thenReturn(List.of(task));

        TaskReorderRequest request =
            new TaskReorderRequest(targetCategoryId, taskIds, null);

        ApiException e = assertThrows(ApiException.class, () -> taskService.reorderTasks(1L, 1L, request));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals(ApiErrorCode.TASK_NEITHER_MOVED_OR_IN_TARGET_CATEGORY, e.getCode());

        verify(taskRepository, never()).countByCategoryId(targetCategoryId);
        verify(boardRevisionService, never()).incrementRevision(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void reorderTasksMovedTaskShouldBeInTaskIds() {
        long targetCategoryId = 1L;
        long sourceCategoryId = 2L;

        Board board = new Board();

        Category targetCategory = new Category();
        targetCategory.setId(targetCategoryId);

        Category sourceCategory = new Category();
        sourceCategory.setId(sourceCategoryId);

        Task movedTask = new Task();
        movedTask.setId(1L);
        movedTask.setCategory(sourceCategory);

        Task otherTask = new Task();
        otherTask.setId(2L);
        otherTask.setCategory(targetCategory);

        List<Long> taskIds = List.of(2L);

        when(boardRepository.findByIdForReorderWithLock(1L)).thenReturn(Optional.of(board));
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.findByIdAndBoardId(targetCategoryId, 1L)).thenReturn(Optional.of(targetCategory));

        when(taskRepository.findByIdInAndCategoryBoardId(taskIds, 1L))
            .thenReturn(List.of(otherTask));

        TaskReorderRequest request =
            new TaskReorderRequest(targetCategoryId, taskIds, movedTask.getId());

        ApiException e = assertThrows(
            ApiException.class, () -> taskService.reorderTasks(1L, 1L, request));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals(ApiErrorCode.TASK_MOVED_TASK_SHOULD_BE_IN_TASK_IDS, e.getCode());

        verify(taskRepository, never()).countByCategoryId(targetCategoryId);
        verify(boardRevisionService, never()).incrementRevision(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void reorderTasksMovedTaskShouldBeInDifferentCategory() {
        long targetCategoryId = 1L;

        Board board = new Board();

        Category targetCategory = new Category();
        targetCategory.setId(targetCategoryId);

        Task movedTask = new Task();
        movedTask.setId(1L);
        movedTask.setCategory(targetCategory);

        List<Long> taskIds = List.of(1L);

        when(boardRepository.findByIdForReorderWithLock(1L)).thenReturn(Optional.of(board));
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.findByIdAndBoardId(targetCategoryId, 1L)).thenReturn(Optional.of(targetCategory));

        when(taskRepository.findByIdInAndCategoryBoardId(taskIds, 1L))
            .thenReturn(List.of(movedTask));

        TaskReorderRequest request =
            new TaskReorderRequest(targetCategoryId, taskIds, movedTask.getId());

        ApiException e = assertThrows(ApiException.class, () -> taskService.reorderTasks(1L, 1L, request));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals(ApiErrorCode.TASK_MOVED_TASK_MUST_BE_IN_DIFF_CATEGORY, e.getCode());

        verify(taskRepository, never()).countByCategoryId(targetCategoryId);
        verify(boardRevisionService, never()).incrementRevision(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void reorderTasksNotAllCategoryTasksWereSpecified() {
        long targetCategoryId = 1L;

        Board board = new Board();

        Category targetCategory = new Category();
        targetCategory.setId(targetCategoryId);

        Task task = new Task();
        task.setId(1L);
        task.setCategory(targetCategory);

        List<Long> taskIds = List.of(1L);

        when(boardRepository.findByIdForReorderWithLock(1L)).thenReturn(Optional.of(board));
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.findByIdAndBoardId(targetCategoryId, 1L)).thenReturn(Optional.of(targetCategory));

        when(taskRepository.findByIdInAndCategoryBoardId(taskIds, 1L))
            .thenReturn(List.of(task));

        when(taskRepository.countByCategoryId(targetCategoryId)).thenReturn(2L);

        TaskReorderRequest request =
            new TaskReorderRequest(targetCategoryId, taskIds, null);

        ApiException e = assertThrows(ApiException.class, () -> taskService.reorderTasks(1L, 1L, request));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals(ApiErrorCode.TASK_REORDER_MISSING_TASK, e.getCode());

        verify(boardRevisionService, never()).incrementRevision(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void reorderTasksSuccessfulSameCategory() {
        long targetCategoryId = 1L;

        Category targetCategory = new Category();
        targetCategory.setId(targetCategoryId);

        Task task1 = new Task();
        task1.setId(1L);
        task1.setCategory(targetCategory);
        task1.setPosition(0);

        Task task2 = new Task();
        task2.setId(2L);
        task2.setCategory(targetCategory);
        task2.setPosition(1);

        Task task3 = new Task();
        task3.setId(3L);
        task3.setCategory(targetCategory);
        task3.setPosition(2);

        List<Long> taskIds = List.of(3L, 2L, 1L);

        when(boardRepository.findByIdForReorderWithLock(1L)).thenReturn(Optional.of(new Board()));
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.findByIdAndBoardId(targetCategoryId, 1L)).thenReturn(Optional.of(targetCategory));

        when(taskRepository.findByIdInAndCategoryBoardId(taskIds, 1L))
            .thenReturn(List.of(task1, task2, task3));

        when(taskRepository.countByCategoryId(targetCategoryId)).thenReturn(3L);

        TaskReorderRequest request =
            new TaskReorderRequest(targetCategoryId, taskIds, null);

        taskService.reorderTasks(1L, 1L, request);

        assertEquals(2, task1.getPosition());
        assertEquals(1, task2.getPosition());
        assertEquals(0, task3.getPosition());

        verify(taskRepository, never()).flush();
        verify(applicationEventPublisher).publishEvent(any(TasksReorderedEvent.class));
    }

    @Test
    void reorderTasksSuccessfulWithMovedTask() {
        long targetCategoryId = 1L;
        long sourceCategoryId = 2L;

        Category targetCategory = new Category();
        targetCategory.setId(targetCategoryId);

        Category sourceCategory = new Category();
        sourceCategory.setId(sourceCategoryId);

        Task task1 = new Task();
        task1.setId(1L);
        task1.setCategory(targetCategory);
        task1.setPosition(0);

        Task task2 = new Task();
        task2.setId(2L);
        task2.setCategory(targetCategory);
        task2.setPosition(1);

        Task movedTask = new Task();
        movedTask.setId(3L);
        movedTask.setCategory(sourceCategory);
        movedTask.setPosition(0);

        Task otherSourceTask = new Task();
        otherSourceTask.setId(4L);
        otherSourceTask.setCategory(sourceCategory);
        otherSourceTask.setPosition(1);

        List<Long> taskIds = List.of(3L, 2L, 1L);

        when(boardRepository.findByIdForReorderWithLock(1L)).thenReturn(Optional.of(new Board()));
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.findByIdAndBoardId(targetCategoryId, 1L)).thenReturn(Optional.of(targetCategory));

        when(taskRepository.findByIdInAndCategoryBoardId(taskIds, 1L))
            .thenReturn(List.of(task1, task2, movedTask));

        when(taskRepository.countByCategoryId(targetCategoryId)).thenReturn(2L);

        // Called for normalizing.
        when(taskRepository.findAllByCategoryIdOrderByPositionAsc(sourceCategoryId))
            .thenReturn(List.of(otherSourceTask));

        TaskReorderRequest request =
            new TaskReorderRequest(targetCategoryId, taskIds, movedTask.getId());

        taskService.reorderTasks(1L, 1L, request);

        // Verify moved task changed category.
        assertEquals(targetCategory, movedTask.getCategory());

        // Verify target category positions.
        assertEquals(2, task1.getPosition());
        assertEquals(1, task2.getPosition());
        assertEquals(0, movedTask.getPosition());

        // Verify source category was normalized.
        verify(taskRepository).flush();
        verify(taskRepository).findAllByCategoryIdOrderByPositionAsc(sourceCategoryId);

        assertEquals(0, otherSourceTask.getPosition());

        verify(applicationEventPublisher).publishEvent(any(TasksReorderedEvent.class));
    }

    @Test
    void deleteTaskLacksAuthorization() {
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(false);

        ApiException e = assertThrows(ApiException.class, () -> taskService.deleteTask(1L, 1L, 1L));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        verify(taskRepository, never()).delete(any());
        verify(boardRevisionService, never()).incrementRevision(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void deleteTaskNotFound() {
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(taskRepository.findByIdAndCategoryBoardId(1L, 1L)).thenReturn(Optional.empty());

        ApiException e = assertThrows(ApiException.class, () -> taskService.deleteTask(1L, 1L, 1L));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        verify(taskRepository, never()).delete(any());
        verify(boardRevisionService, never()).incrementRevision(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void deleteTaskSuccessful() {

        Long deleteCategoryId = 2L;

        Category deletedCategory = new Category();
        deletedCategory.setId(deleteCategoryId);

        Task deletedTask = new Task();
        deletedTask.setId(deleteCategoryId);
        deletedTask.setPosition(1);
        deletedTask.setCategory(deletedCategory);

        // Remaining tasks.
        Task task1 = new Task();
        task1.setId(1L);
        task1.setPosition(0);

        Task task2 = new Task();
        task2.setId(3L);
        task2.setPosition(2);

        Task task3 = new Task();
        task3.setId(4L);
        task3.setPosition(3);

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(taskRepository.findByIdAndCategoryBoardId(deleteCategoryId, 1L))
            .thenReturn(Optional.of(deletedTask));
        when(taskRepository.findAllByCategoryIdOrderByPositionAsc(deleteCategoryId))
            .thenReturn(List.of(task1, task2, task3));

        taskService.deleteTask(1L, 1L, deleteCategoryId);

        verify(taskRepository).delete(deletedTask);
        verify(taskRepository).flush();

        // Verify positions are normalized.
        assertEquals(0, task1.getPosition());
        assertEquals(1, task2.getPosition());
        assertEquals(2, task3.getPosition());

        verify(applicationEventPublisher).publishEvent(any(TaskDeletedEvent.class));
    }

    @Test
    void searchAssigneesLacksBoardAuthorization() {

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(false);

        ApiException e =
            assertThrows(ApiException.class, () -> taskService.searchAssignees(1L, 1L, 1L, "searchUsername"));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        verify(taskRepository, never()).findByIdAndCategoryBoardId(any(), any());
    }

    @Test
    void searchAssigneesTaskNotFound() {

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(taskRepository.findByIdAndCategoryBoardId(1L, 1L)).thenReturn(Optional.empty());

        ApiException e =
            assertThrows(ApiException.class, () -> taskService.searchAssignees(1L, 1L, 1L, "searchUsername"));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        verify(userRepository, never()).searchAssignees(any(), any(), any(), any());
    }

    @Test
    void searchAssigneesTaskSuccessful() {

        User assignedUser = new User();
        assignedUser.setId(2L);
        assignedUser.setUsername("assignedUser");

        User unassignedUser = new User();
        unassignedUser.setId(3L);
        unassignedUser.setUsername("unassignedUser");

        Task task = new Task();
        task.setAssignees(Set.of(assignedUser));

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(taskRepository.findByIdAndCategoryBoardId(1L, 1L)).thenReturn(Optional.of(task));
        when(userRepository.searchAssignees(
            "searchUsername", null, 1L, PageRequest.of(0, 10)
        )).thenReturn(List.of(assignedUser, unassignedUser));

        List<TaskAssigneeSearchResponse> response = taskService.searchAssignees(1L, 1L, 1L, "searchUsername");

        assertEquals(2, response.size());

        assertEquals(2L, response.getFirst().id());
        assertEquals("assignedUser", response.get(0).username());
        assertTrue(response.get(0).alreadyAssigned());

        assertEquals(3L, response.get(1).id());
        assertEquals("unassignedUser", response.get(1).username());
        assertFalse(response.get(1).alreadyAssigned());

        verify(boardRepository).isUserAllowed(1L, 1L);
        verify(taskRepository).findByIdAndCategoryBoardId(1L, 1L);
        verify(userRepository).searchAssignees(
            "searchUsername", null, 1L, PageRequest.of(0, 10)
        );
    }
}
