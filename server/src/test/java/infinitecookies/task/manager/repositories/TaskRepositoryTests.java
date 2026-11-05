package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.Board;
import infinitecookies.task.manager.models.Category;
import infinitecookies.task.manager.models.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void findAllByBoardIdReturnsTasksOrderedByCategoryAndPosition() {
        Board board = new Board();
        entityManager.persist(board);

        Category category1 = createCategory(board);
        Category category2 = createCategory(board);

        Task task1 = createTask(category1, 2);
        Task task2 = createTask(category1, 1);
        Task task3 = createTask(category2, 2);
        Task task4 = createTask(category2, 1);

        entityManager.flush();
        entityManager.clear();

        List<Task> result = taskRepository.findAllByBoardId(board.getId());

        assertEquals(
            List.of(
                task2.getId(),
                task1.getId(),
                task4.getId(),
                task3.getId()
            ),
            result.stream()
                .map(Task::getId)
                .toList()
        );
    }

    private Category createCategory(Board board) {
        Category category = new Category();
        category.setBoard(board);
        category.setName("Test category");
        category.setPosition(0);

        entityManager.persist(category);
        return category;
    }

    private Task createTask(Category category, int position) {
        Task task = new Task();
        task.setCategory(category);
        task.setName("Task " + position);
        task.setPosition(position);

        entityManager.persist(task);
        return task;
    }
}
