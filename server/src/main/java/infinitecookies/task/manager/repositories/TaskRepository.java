package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    long countByCategoryId(Long categoryId);

    List<Task> findAllByCategoryIdOrderByPositionAsc(Long categoryId);

    Optional<Task> findByIdAndCategoryBoardId(Long id, Long boardId);

    void deleteById(Long id);

}
