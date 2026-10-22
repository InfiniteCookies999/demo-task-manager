package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    long countByCategoryId(Long categoryId);

    @Query("""
        SELECT t
        FROM Task t
        JOIN t.category c
        WHERE c.board.id = :boardId
        ORDER BY c.id ASC, t.position ASC
        """)
    List<Task> findAllByBoardId(Long boardId);

    List<Task> findAllByCategoryIdOrderByPositionAsc(Long categoryId);

    Optional<Task> findByIdAndCategoryBoardId(Long id, Long boardId);

    List<Task> findByIdInAndCategoryBoardId(List<Long> taskIds, Long boardId);

    void deleteById(Long id);

}
