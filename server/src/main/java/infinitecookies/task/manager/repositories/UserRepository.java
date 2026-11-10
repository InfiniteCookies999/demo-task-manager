package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findBySessionId(String sessionId);

    @Query("""
        SELECT u
        FROM User u
        WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))
              AND (
                  :userId IS NULL OR
                  CAST(u.id AS string) LIKE CONCAT('%', CAST(:userId AS string), '%')
              )
        ORDER BY LOWER(u.username) ASC, u.id ASC
    """)
    List<User> searchUsersForInvite(
        @Param("username") String username,
        @Param("userId") Long userId,
        Pageable pageable
    );

    @Query("""
        SELECT u
        FROM Board b
        JOIN b.allowedUsers u
        WHERE b.id = :boardId AND (
                LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))
                AND (
                    :userId IS NULL OR
                    CAST(u.id AS string) LIKE CONCAT('%', CAST(:userId AS string), '%')
                )
            )
        ORDER BY LOWER(u.username) ASC, u.id ASC
    """)
    List<User> searchAssignees(
        @Param("username") String username,
        @Param("userId") Long userId,
        @Param("boardId") Long boardId,
        Pageable pageable
    );

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
