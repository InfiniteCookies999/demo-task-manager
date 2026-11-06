package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.Board;
import infinitecookies.task.manager.models.Category;
import infinitecookies.task.manager.models.Task;
import infinitecookies.task.manager.models.User;
import infinitecookies.task.manager.repositories.projections.TaskProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
public class TaskRepositoryTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void findAllAssigneesByIdAndBoardIdReturnsUsersOrderedByUsername() {

        Board creatorBoard = new Board();
        entityManager.persist(creatorBoard);

        Board user1Board = new Board();
        entityManager.persist(user1Board);

        Board user2Board = new Board();
        entityManager.persist(user2Board);

        Board user3Board = new Board();
        entityManager.persist(user3Board);

        User creator = createUser(creatorBoard);

        Category category = createCategory(creatorBoard);

        Task task = createTask(creator, category, 1);

        User user1 = createUser(user1Board, "zuser", "session1");
        User user2 = createUser(user2Board, "auser", "session2");
        User user3 = createUser(user3Board, "buser", "session3");

        task.getAssignees().add(user1);
        task.getAssignees().add(user2);
        task.getAssignees().add(user3);

        entityManager.flush();
        entityManager.clear();

        List<User> result = taskRepository
            .findAllAssigneesByIdAndBoardIdAndBoardId(task.getId(), creatorBoard.getId());

        assertEquals(
            List.of("auser", "buser", "zuser"),
            result.stream()
                .map(User::getUsername)
                .toList()
        );
    }

    @Test
    void findTasksByBoardIdReturnsCorrectProjections() {

        Board board1 = new Board();
        entityManager.persist(board1);

        Board board2 = new Board();
        entityManager.persist(board2);

        Board board3 = new Board();
        entityManager.persist(board3);

        User creator = createUser(board1);
        User assignedUser = createUser(board2, "assignedUser", "session1");
        User unassignedUser = createUser(board3, "unassignedUser", "session2");

        Category category = createCategory(board1);

        Task task1 = createTask(creator, category, 1);
        task1.setPosition(2);
        task1.setCompleted(true);
        task1.getAssignees().add(assignedUser);

        Task task2 = createTask(creator, category, 2);
        task2.setCompleted(false);
        task2.setPosition(0);

        Task task3 = createTask(creator, category, 3);
        task3.getAssignees().add(unassignedUser);
        task3.setPosition(1);

        // Task belonging to another board should not be returned.
        Category otherCategory = createCategory(board2);
        Task otherTask = createTask(creator, otherCategory, 1);
        otherTask.getAssignees().add(assignedUser);

        entityManager.flush();
        entityManager.clear();

        List<TaskProjection> result = taskRepository.findTasksByBoardId(assignedUser.getId(), board1.getId());

        assertEquals(3, result.size());

        TaskProjection projection1 = result.get(2);
        TaskProjection projection2 = result.get(0);
        TaskProjection projection3 = result.get(1);

        // Task 1: Assigned to the requesting user.
        assertEquals(task1.getId(), projection1.getId());
        assertEquals(category.getId(), projection1.getCategoryId());
        assertEquals("Task 1", projection1.getName());
        assertTrue(projection1.getCompleted());
        assertEquals(creator.getId(), projection1.getCreatedByUserId());
        assertEquals(creator.getUsername(), projection1.getCreatedByUsername());
        assertTrue(projection1.getAssignedToYou());

        // Task 2: Not assigned to anyone.
        assertEquals(task2.getId(), projection2.getId());
        assertFalse(projection2.getCompleted());
        assertFalse(projection2.getAssignedToYou());

        // Task 3: Assigned to a different user.
        assertEquals(task3.getId(), projection3.getId());
        assertFalse(projection3.getAssignedToYou());
    }

    private User createUser(Board board) {

        User user = new User();
        user.setUsername("testUser");
        user.setSessionId("testSession");
        user.setOwnedBoard(board);
        entityManager.persist(user);

        return user;
    }

    private User createUser(Board board, String username, String sessionId) {

        User user = new User();
        user.setUsername(username);
        user.setSessionId(sessionId);
        user.setOwnedBoard(board);
        entityManager.persist(user);

        return user;
    }

    private Category createCategory(Board board) {
        Category category = new Category();
        category.setBoard(board);
        category.setName("Test category");
        category.setPosition(0);

        entityManager.persist(category);
        return category;
    }

    private Task createTask(User user, Category category, int position) {
        Task task = new Task();
        task.setCategory(category);
        task.setName("Task " + position);
        task.setPosition(position);
        task.setCreatedByUser(user);

        entityManager.persist(task);
        return task;
    }
}
