package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    long countBySessionId(String sessionId);

    List<Category> findAllBySessionIdOrderByPositionAsc(String sessionId);

    Optional<Category> findByIdAndSessionId(Long id, String sessionId);

    int deleteByIdAndSessionId(Long id, String sessionId);

}
