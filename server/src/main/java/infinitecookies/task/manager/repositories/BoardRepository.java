package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.Board;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {

    @Query("""
        SELECT COUNT(u) > 0
        FROM Board b
        JOIN b.allowedUsers u
        WHERE b.id = :boardId AND u.id = :userId
        """)
    boolean isUserAllowed(Long boardId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Board b WHERE b.id = :boardId")
    Optional<Board> findByIdForReorderWithLock(@Param("boardId") Long boardId);

    List<Board> findByAllowedUsers_Id(Long userId);

}
