package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.Task;
import infinitecookies.task.manager.models.User;
import infinitecookies.task.manager.repositories.projections.TaskProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    long countByCategoryId(Long categoryId);

    /**
     * Retrieves the tasks for a given board determining if for each task the task is assigned to
     * the user. Ordered first by category ID and then task position.
     */
    @Query("""
        SELECT
            t.id AS id,
            t.category.id AS categoryId,
            t.name AS name,
            t.description AS description,
            t.completed AS completed,
            t.position AS position,
            t.createdByUser.id AS createdByUserId,
            t.createdByUser.username AS createdByUsername,
            CASE WHEN EXISTS (
                SELECT 1
                FROM t.assignees a
                WHERE a.id = :userId
            ) THEN true ELSE false END
                AS assignedToYou
        FROM Task t
        WHERE t.category.board.id = :boardId
        ORDER BY t.category.position ASC, t.position
    """)
    List<TaskProjection> findTasksByBoardId(@Param("userId") Long userId, @Param("boardId") Long boardId);

    /**
     * @return All the users assigned to the task ordered by their username.
     */
    @Query("""
        SELECT u
        FROM Task t
        JOIN t.assignees u
        WHERE t.id = :id AND t.category.board.id = :boardId
        ORDER BY LOWER(u.username) ASC
    """)
    List<User> findAllAssigneesByIdAndBoardIdAndBoardId(@Param("id") Long id, @Param("boardId") Long boardId);

    List<Task> findAllByCategoryIdOrderByPositionAsc(Long categoryId);

    Optional<Task> findByIdAndCategoryBoardId(Long id, Long boardId);

    List<Task> findByIdInAndCategoryBoardId(List<Long> taskIds, Long boardId);

    void deleteById(Long id);

}
