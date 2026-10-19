package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BoardRepository extends JpaRepository<Board, Long> {

    @Query("""
        SELECT COUNT(u) > 0
        FROM Board b
        JOIN b.allowedUsers u
        WHERE b.id = :boardId AND u.id = :userId
        """)
    boolean isUserAllowed(Long boardId, Long userId);

}
