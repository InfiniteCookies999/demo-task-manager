package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    long countBySessionIdAndCategoryId(String sessionId, Long categoryId);

    List<Task> findAllBySessionIdAndCategoryIdOrderByPositionAsc(String sessionId, Long categoryId);

    Optional<Task> findByIdAndSessionId(Long id, String sessionId);

}
