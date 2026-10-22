package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    long countByBoardId(Long boardId);

    List<Category> findAllByBoardIdOrderByPositionAsc(Long boardId);

    Optional<Category> findByIdAndBoardId(Long id, Long boardId);

    int deleteByIdAndBoardId(Long id, Long boardId);

    List<Category> findAllByBoardId(Long boardId);

}
