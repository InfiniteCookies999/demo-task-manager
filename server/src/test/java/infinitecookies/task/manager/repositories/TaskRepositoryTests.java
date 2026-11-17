package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.entity.Board;
import infinitecookies.task.manager.models.entity.Category;
import infinitecookies.task.manager.models.entity.Task;
import infinitecookies.task.manager.models.entity.User;
import infinitecookies.task.manager.repositories.projections.TaskProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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

        User creator = createUser();

        Board creatorBoard = new Board();
        creatorBoard.setOwner(creator);
        entityManager.persist(creatorBoard);

        Category category = createCategory(creatorBoard);

        Task task = createTask(creator, category, 1);

        User user1 = createUser("zuser", "session1");
        User user2 = createUser("auser", "session2");
        User user3 = createUser("buser", "session3");

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

        User creator = createUser();
        User assignedUser = createUser("assignedUser", "session1");
        User unassignedUser = createUser("unassignedUser", "session2");

        Board creatorBoard = new Board();
        creatorBoard.setOwner(creator);
        entityManager.persist(creatorBoard);

        Board otherBoard = new Board();
        otherBoard.setOwner(unassignedUser);
        entityManager.persist(otherBoard);

        Category category = createCategory(creatorBoard);

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
        Category otherCategory = createCategory(otherBoard);
        Task otherTask = createTask(creator, otherCategory, 1);
        otherTask.getAssignees().add(assignedUser);

        entityManager.flush();
        entityManager.clear();

        List<TaskProjection> result = taskRepository.findTasksByBoardId(assignedUser.getId(), creatorBoard.getId());

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

    private User createUser() {

        User user = new User();
        user.setUsername("testUser");
        user.setSessionId("testSession");
        entityManager.persist(user);

        return user;
    }

    private User createUser(String username, String sessionId) {

        User user = new User();
        user.setUsername(username);
        user.setSessionId(sessionId);
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
