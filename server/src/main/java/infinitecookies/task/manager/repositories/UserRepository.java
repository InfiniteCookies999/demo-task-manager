package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findBySessionId(String sessionId);

    List<User> findTop10ByUsernameContainingIgnoreCaseOrderByUsernameAsc(String username);

    @Query("""
        SELECT u
        FROM Board b
        JOIN b.allowedUsers u
        WHERE b.id = :boardId AND
            LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))
        ORDER BY u.username ASC
    """)
    List<User> findTop10ByUsernameIgnoreCaseBelongingToBoardId(String username, Long boardId);

    @Query("""
        SELECT u
        FROM Board b
        JOIN b.allowedUsers u
        WHERE b.id = :boardId AND u.id IN :userIds
    """)
    List<User> findAllowedUsersByBoardIdAndUserIds(
        @Param("boardId") Long boardId,
        @Param("userIds") Set<Long> userIds
    );

}
