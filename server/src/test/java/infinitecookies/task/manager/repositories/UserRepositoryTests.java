package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.entity.Board;
import infinitecookies.task.manager.models.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Testcontainers
public class UserRepositoryTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void searchAssigneesByOnlyUsername() {

        Board board1 = new Board();
        entityManager.persist(board1);

        Board board2 = new Board();
        entityManager.persist(board2);

        Board board3 = new Board();
        entityManager.persist(board3);

        Board board4 = new Board();
        entityManager.persist(board4);

        User user1 = createUser(board1, "testZebra");
        User user2 = createUser(board2, "testApple");
        User user3 = createUser(board3, "testBanana");
        User user4 = createUser(board4, "tesNotFound");

        board1.setAllowedUsers(Set.of(user1, user2, user3, user4));

        Long boardId = board1.getId();
        List<String> expected = List.of(
            user2.getUsername(),
            user3.getUsername(),
            user1.getUsername()
        );

        entityManager.flush();
        entityManager.clear();

        List<User> result = userRepository.searchAssignees("test", null, boardId, PageRequest.of(0, 10));

        assertEquals(
            expected,
            result.stream()
                .map(User::getUsername)
                .toList()
        );
    }

    @Test
    void searchAssigneesByOnlyId() {

        Board board1 = new Board();
        entityManager.persist(board1);

        Board board2 = new Board();
        entityManager.persist(board2);

        Board board3 = new Board();
        entityManager.persist(board3);

        Board board4 = new Board();
        entityManager.persist(board4);

        User user1 = createUser(board1, "testUserA");
        User user2 = createUser(board2, "testUserB");
        User user3 = createUser(board3, "testUserC");

        board1.setAllowedUsers(Set.of(user1, user2, user3));

        entityManager.flush();

        Long boardId = board1.getId();
        List<Long> expected = List.of(user2.getId());

        Long idSearch = user2.getId();

        entityManager.clear();

        List<User> result = userRepository.searchAssignees("", idSearch, boardId, PageRequest.of(0, 10));

        assertEquals(
            expected,
            result.stream()
                .map(User::getId)
                .toList()
        );
    }

    @Test
    void searchAssigneesByUsernameAndId() {

        Board board1 = new Board();
        entityManager.persist(board1);

        Board board2 = new Board();
        entityManager.persist(board2);

        Board board3 = new Board();
        entityManager.persist(board3);

        Board board4 = new Board();
        entityManager.persist(board4);

        User user1 = createUser(board1, "testUserB");
        User user2 = createUser(board2, "testUserA");
        User user3 = createUser(board3, "testUserC");

        String searchIdString = user1.getId().toString().substring(0, 1);
        Long searchId = Long.parseLong(searchIdString);

        board1.setAllowedUsers(Set.of(user1, user2, user3));

        entityManager.flush();

        Long boardId = board1.getId();

        // We cannot know beforehand what the database IDs will be and so
        // since the search uses a LIKE we must also check if any of the
        // auto generated IDs are in the ID search.
        List<Long> expectedIds = Stream.of(user1, user2, user3)
            .filter(user -> user.getId().toString().contains(searchIdString))
            .sorted(Comparator.comparing(
                User::getUsername,
                String.CASE_INSENSITIVE_ORDER
            ).thenComparing(User::getId))
            .map(User::getId)
            .toList();

        entityManager.clear();

        List<User> result = userRepository.searchAssignees("test", searchId, boardId, PageRequest.of(0, 10));

        assertEquals(
            expectedIds,
            result.stream()
                .map(User::getId)
                .toList()
        );
    }

    @Test
    void searchAssigneesShouldBeCaseInsensitive() {

        Board board1 = new Board();
        entityManager.persist(board1);

        Board board2 = new Board();
        entityManager.persist(board2);

        Board board3 = new Board();
        entityManager.persist(board3);

        User user1 = createUser(board1, "TestUser");
        User user2 = createUser(board2, "TESTUSER");
        User user3 = createUser(board3, "OtherUser");

        board1.setAllowedUsers(Set.of(user1, user2, user3));

        entityManager.flush();

        Long boardId = board1.getId();

        List<String> expected = Stream.of(user1, user2)
            .sorted(Comparator.comparing(User::getId))
            .map(User::getUsername)
            .toList();

        entityManager.clear();

        List<User> result = userRepository.searchAssignees(
            "test",
            null,
            boardId,
            PageRequest.of(0, 10)
        );

        assertEquals(
            expected,
            result.stream()
                .map(User::getUsername)
                .toList()
        );
    }

    @Test
    void searchAssigneesReturnsAtMost10Results() {

        Set<User> users = IntStream.range(0, 14)
            .mapToObj(i -> {
                Board board = new Board();
                entityManager.persist(board);
                return createUser(board, "testUser" + i);
            })
            .collect(Collectors.toSet());

        Board board = users.stream().findFirst().orElseThrow().getOwnedBoard();
        board.setAllowedUsers(users);

        entityManager.flush();
        entityManager.clear();

        List<User> result = userRepository.searchAssignees(
            "test",
            null,
            board.getId(),
            PageRequest.of(0, 10)
        );

        assertEquals(10, result.size());
    }


    @Test
    void searchUsersForInviteByOnlyUsername() {

        Board board1 = new Board();
        entityManager.persist(board1);

        Board board2 = new Board();
        entityManager.persist(board2);

        Board board3 = new Board();
        entityManager.persist(board3);

        Board board4 = new Board();
        entityManager.persist(board4);

        User user1 = createUser(board1, "testZebra");
        User user2 = createUser(board2, "testApple");
        User user3 = createUser(board3, "testBanana");
        createUser(board4, "tesNotFound");

        List<String> expected = List.of(
            user2.getUsername(),
            user3.getUsername(),
            user1.getUsername()
        );

        entityManager.flush();
        entityManager.clear();

        List<User> result = userRepository.searchUsersForInvite("test", null, PageRequest.of(0, 10));

        assertEquals(
            expected,
            result.stream()
                .map(User::getUsername)
                .toList()
        );
    }

    @Test
    void searchForInviteByOnlyId() {

        Board board1 = new Board();
        entityManager.persist(board1);

        Board board2 = new Board();
        entityManager.persist(board2);

        Board board3 = new Board();
        entityManager.persist(board3);

        Board board4 = new Board();
        entityManager.persist(board4);

        createUser(board1, "testUserA");
        User user2 = createUser(board2, "testUserB");
        createUser(board3, "testUserC");

        entityManager.flush();

        List<Long> expected = List.of(user2.getId());

        Long idSearch = user2.getId();

        entityManager.clear();

        List<User> result = userRepository.searchUsersForInvite("", idSearch, PageRequest.of(0, 10));

        assertEquals(
            expected,
            result.stream()
                .map(User::getId)
                .toList()
        );
    }

    @Test
    void searchForInviteByUsernameAndId() {

        Board board1 = new Board();
        entityManager.persist(board1);

        Board board2 = new Board();
        entityManager.persist(board2);

        Board board3 = new Board();
        entityManager.persist(board3);

        Board board4 = new Board();
        entityManager.persist(board4);

        User user1 = createUser(board1, "testUserB");
        User user2 = createUser(board2, "testUserA");
        User user3 = createUser(board3, "testUserC");

        String searchIdString = user1.getId().toString().substring(0, 1);
        Long searchId = Long.parseLong(searchIdString);

        entityManager.flush();

        // We cannot know beforehand what the database IDs will be and so
        // since the search uses a LIKE we must also check if any of the
        // auto generated IDs are in the ID search.
        List<Long> expectedIds = Stream.of(user1, user2, user3)
            .filter(user -> user.getId().toString().contains(searchIdString))
            .sorted(Comparator.comparing(
                User::getUsername,
                String.CASE_INSENSITIVE_ORDER
            ).thenComparing(User::getId))
            .map(User::getId)
            .toList();

        entityManager.clear();

        List<User> result = userRepository.searchUsersForInvite("test", searchId, PageRequest.of(0, 10));

        assertEquals(
            expectedIds,
            result.stream()
                .map(User::getId)
                .toList()
        );
    }

    @Test
    void searchForInviteShouldBeCaseInsensitive() {

        Board board1 = new Board();
        entityManager.persist(board1);

        Board board2 = new Board();
        entityManager.persist(board2);

        Board board3 = new Board();
        entityManager.persist(board3);

        User user1 = createUser(board1, "TestUser");
        User user2 = createUser(board2, "TESTUSER");
        createUser(board3, "OtherUser");

        entityManager.flush();

        List<String> expected = Stream.of(user1, user2)
            .sorted(Comparator.comparing(User::getId))
            .map(User::getUsername)
            .toList();

        entityManager.clear();

        List<User> result = userRepository.searchUsersForInvite(
            "test",
            null,
            PageRequest.of(0, 10)
        );

        assertEquals(
            expected,
            result.stream()
                .map(User::getUsername)
                .toList()
        );
    }

    @Test
    void searchForInviteReturnsAtMost10Results() {

        IntStream.range(0, 14).forEach(i -> {
            Board board = new Board();
            entityManager.persist(board);
            createUser(board, "testUser" + i);
        });

        entityManager.flush();
        entityManager.clear();

        List<User> result = userRepository.searchUsersForInvite(
            "test",
            null,
            PageRequest.of(0, 10)
        );

        assertEquals(10, result.size());
    }

    private User createUser(Board board, String username) {

        User user = new User();
        user.setUsername(username);
        user.setSessionId(username + "session");
        user.setOwnedBoard(board);
        entityManager.persist(user);

        return user;
    }
}
