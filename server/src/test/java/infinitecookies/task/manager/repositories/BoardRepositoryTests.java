package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.Board;
import infinitecookies.task.manager.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Testcontainers
public class BoardRepositoryTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void isUserAllowedForAllowedUser() {

        Board board = new Board();

        User user = new User();
        user.setSessionId("3442123");
        user.setUsername("testUser");
        user.setOwnedBoard(board);

        entityManager.persist(user);

        board.setAllowedUsers(Set.of(user));
        entityManager.persist(board);

        entityManager.flush();

        assertTrue(boardRepository.isUserAllowed(board.getId(), user.getId()));
    }

    @Test
    void isUserAllowedForNotAllowedUser() {

        Board board = new Board();
        Board otherBoard = new Board();

        User user = new User();
        user.setSessionId("3442123");
        user.setUsername("testUser");
        user.setOwnedBoard(board);

        entityManager.persist(user);

        board.setAllowedUsers(Set.of(user));
        entityManager.persist(board);
        entityManager.persist(otherBoard);

        entityManager.flush();

        assertFalse(boardRepository.isUserAllowed(otherBoard.getId(), user.getId()));
    }
}
