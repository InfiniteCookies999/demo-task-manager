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

    @Autowired
    private BoardRepository boardRepository;

    @Test
    void searchAssigneesByOnlyUsername() {

        User user1 = createUser("testZebra");
        User user2 = createUser("testApple");
        User user3 = createUser("testBanana");
        User user4 = createUser("tesNotFound");

        Board board = new Board();
        board.setOwner(user1);
        board.setAllowedUsers(Set.of(user1, user2, user3, user4));
        entityManager.persist(board);

        Long boardId = board.getId();
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

        User user1 = createUser("testUserA");
        User user2 = createUser("testUserB");
        User user3 = createUser("testUserC");

        Board board = new Board();
        board.setOwner(user1);
        board.setAllowedUsers(Set.of(user1, user2, user3));
        entityManager.persist(board);

        entityManager.flush();

        Long boardId = board.getId();
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

        User user1 = createUser("testUserB");
        User user2 = createUser("testUserA");
        User user3 = createUser("testUserC");

        String searchIdString = user1.getId().toString().substring(0, 1);
        Long searchId = Long.parseLong(searchIdString);

        Board board = new Board();
        board.setOwner(user1);
        board.setAllowedUsers(Set.of(user1, user2, user3));
        entityManager.persist(board);

        entityManager.flush();

        Long boardId = board.getId();

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

        User user1 = createUser("TestUser");
        User user2 = createUser("TESTUSER");
        User user3 = createUser("OtherUser");

        Board board = new Board();
        board.setOwner(user1);
        board.setAllowedUsers(Set.of(user1, user2, user3));
        entityManager.persist(board);

        entityManager.flush();

        Long boardId = board.getId();

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
            .mapToObj(i -> createUser("testUser" + i))
            .collect(Collectors.toSet());

        Board board = new Board();
        board.setOwner(users.stream().findFirst().orElseThrow());
        board.setAllowedUsers(users);
        entityManager.persist(board);

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

        User user1 = createUser("testZebra");
        User user2 = createUser("testApple");
        User user3 = createUser("testBanana");
        createUser("tesNotFound");

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

        createUser("testUserA");
        User user2 = createUser("testUserB");
        createUser("testUserC");

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

        User user1 = createUser("testUserB");
        User user2 = createUser("testUserA");
        User user3 = createUser("testUserC");

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

        User user1 = createUser("TestUser");
        User user2 = createUser("TESTUSER");
        createUser("OtherUser");

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
            User user = createUser("testUser" + i);
            Board board = new Board();
            board.setOwner(user);
            entityManager.persist(board);
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

    private User createUser(String username) {

        User user = new User();
        user.setUsername(username);
        user.setSessionId(username + "session");
        entityManager.persist(user);

        return user;
    }
}
